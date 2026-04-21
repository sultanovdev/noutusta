(() => {
    const chatToggle = document.getElementById("chat-toggle");
    const chatPanel = document.getElementById("chat-panel");
    const chatClose = document.getElementById("chat-close");
    const chatForm = document.getElementById("chat-form");
    const chatInput = document.getElementById("chat-input");
    const chatMessages = document.getElementById("chat-messages");
    const chatSubmit = document.getElementById("chat-submit");
    if (!chatToggle || !chatPanel || !chatForm || !chatInput || !chatMessages) {
        return;
    }
    const welcomeMessage = chatToggle.dataset.chatWelcomeMessage || "Salom! Xabaringizni yuboring.";
    const loadingMessage = chatToggle.dataset.chatLoadingMessage || "Yuklanmoqda...";
    const errorMessage = chatToggle.dataset.chatErrorMessage || "Vaqtinchalik xatolik yuz berdi.";
    const fallbackMessage = chatToggle.dataset.chatFallbackMessage || "Xabaringiz qabul qilindi.";

    const inactivityMs = 60000;
    const scrollRevealPx = 500;
    let inactivityTimer;
    let chatRevealed = false;
    let userClosedOnce = false;

    function appendMessage(text, isUser) {
        const bubble = document.createElement("div");
        bubble.className = isUser ? "chat-bubble-user" : "chat-bubble-bot";
        bubble.textContent = text;
        chatMessages.appendChild(bubble);
        chatMessages.scrollTop = chatMessages.scrollHeight;
        return bubble;
    }

    function revealToggle() {
        if (chatRevealed || userClosedOnce) {
            return;
        }
        chatRevealed = true;
        chatToggle.classList.remove("hidden");
        chatToggle.classList.add("pointer-events-auto");
        chatToggle.classList.add("chat-pulse-once");
        setTimeout(() => chatToggle.classList.remove("chat-pulse-once"), 1800);
    }

    function resetInactivityTimer() {
        clearTimeout(inactivityTimer);
        inactivityTimer = setTimeout(revealToggle, inactivityMs);
    }

    function openPanel() {
        chatPanel.classList.remove("hidden");
        requestAnimationFrame(() => chatPanel.classList.add("visible"));
        if (!chatMessages.hasChildNodes()) {
            appendMessage(welcomeMessage, false);
        }
    }

    function closePanel() {
        chatPanel.classList.remove("visible");
        setTimeout(() => chatPanel.classList.add("hidden"), 220);
        userClosedOnce = true;
    }

    async function sendMessage(message) {
        const response = await fetch("/api/chat", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({message})
        });
        if (!response.ok) {
            throw new Error("Request failed");
        }
        return response.json();
    }

    chatToggle.addEventListener("click", openPanel);
    if (chatClose) {
        chatClose.addEventListener("click", closePanel);
    }

    chatForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        const message = chatInput.value.trim();
        if (!message) {
            return;
        }

        appendMessage(message, true);
        chatInput.value = "";
        chatInput.disabled = true;
        if (chatSubmit) {
            chatSubmit.disabled = true;
        }
        const loadingBubble = appendMessage(loadingMessage, false);

        try {
            const data = await sendMessage(message);
            loadingBubble.remove();
            appendMessage(data.reply || fallbackMessage, false);
        } catch (error) {
            loadingBubble.remove();
            appendMessage(errorMessage, false);
        } finally {
            chatInput.disabled = false;
            if (chatSubmit) {
                chatSubmit.disabled = false;
            }
            chatInput.focus();
        }
    });

    ["click", "keydown", "scroll", "touchstart", "mousemove"].forEach((eventName) => {
        window.addEventListener(eventName, resetInactivityTimer, {passive: true});
    });

    window.addEventListener("scroll", () => {
        if (window.scrollY > scrollRevealPx) {
            revealToggle();
        }
    }, {passive: true});

    resetInactivityTimer();
})();
