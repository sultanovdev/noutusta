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
    const handoffButtonLabel = chatToggle.dataset.chatHandoffButton || "Telegramga yozish";

    const inactivityMs = 60000;
    const scrollRevealPx = 500;
    const historyTtlMs = 5 * 60 * 1000;
    const maxStoredMessages = 40;
    const historyStorageKey = "noutusta.chat.history.v1";
    let inactivityTimer;
    let chatRevealed = false;
    let userClosedOnce = false;
    let messageHistory = [];

    function hasSessionStorage() {
        try {
            return typeof window.sessionStorage !== "undefined";
        } catch (error) {
            return false;
        }
    }

    function persistHistory() {
        if (!hasSessionStorage()) {
            return;
        }
        try {
            window.sessionStorage.setItem(historyStorageKey, JSON.stringify({
                version: 1,
                updatedAt: Date.now(),
                messages: messageHistory.slice(-maxStoredMessages)
            }));
        } catch (error) {
            // Ignore storage write failures (quota/private mode)
        }
    }

    function clearPersistedHistory() {
        messageHistory = [];
        if (!hasSessionStorage()) {
            return;
        }
        try {
            window.sessionStorage.removeItem(historyStorageKey);
        } catch (error) {
            // Ignore storage cleanup failures
        }
    }

    function parseStoredHistory(rawValue) {
        if (!rawValue) {
            return [];
        }
        try {
            const parsed = JSON.parse(rawValue);
            if (!parsed || !Array.isArray(parsed.messages) || typeof parsed.updatedAt !== "number") {
                return [];
            }
            if (Date.now() - parsed.updatedAt > historyTtlMs) {
                return [];
            }
            return parsed.messages
                .filter((entry) => entry
                    && typeof entry.text === "string"
                    && typeof entry.isUser === "boolean")
                .map((entry) => ({
                    text: entry.text.slice(0, 500),
                    isUser: entry.isUser,
                    at: typeof entry.at === "number" ? entry.at : parsed.updatedAt
                }))
                .slice(-maxStoredMessages);
        } catch (error) {
            return [];
        }
    }

    function rememberMessage(text, isUser) {
        const normalized = String(text || "").trim();
        if (!normalized) {
            return;
        }
        messageHistory.push({
            text: normalized,
            isUser,
            at: Date.now()
        });
        if (messageHistory.length > maxStoredMessages) {
            messageHistory = messageHistory.slice(-maxStoredMessages);
        }
        persistHistory();
    }

    function restoreHistoryIfAvailable() {
        if (!hasSessionStorage()) {
            return;
        }
        const restoredMessages = parseStoredHistory(window.sessionStorage.getItem(historyStorageKey));
        if (restoredMessages.length === 0) {
            clearPersistedHistory();
            return;
        }

        messageHistory = restoredMessages;
        messageHistory.forEach((entry) => appendMessage(entry.text, entry.isUser, false));
        chatRevealed = true;
        chatToggle.classList.remove("hidden");
        chatToggle.classList.add("pointer-events-auto");
    }

    function appendMessage(text, isUser, persist = true) {
        const bubble = document.createElement("div");
        bubble.className = isUser ? "chat-bubble-user" : "chat-bubble-bot";
        bubble.textContent = text;
        chatMessages.appendChild(bubble);
        chatMessages.scrollTop = chatMessages.scrollHeight;
        if (persist) {
            rememberMessage(text, isUser);
        }
        return bubble;
    }

    function appendActionButton(url, label) {
        if (!url) {
            return;
        }
        const button = document.createElement("a");
        button.href = url;
        button.target = "_blank";
        button.rel = "noopener noreferrer";
        button.className = "chat-bubble-bot chat-action-link";
        button.textContent = label;
        chatMessages.appendChild(button);
        chatMessages.scrollTop = chatMessages.scrollHeight;
        rememberMessage(label + ": " + url, false);
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
            appendMessage(welcomeMessage, false, true);
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
        const loadingBubble = appendMessage(loadingMessage, false, false);

        try {
            const data = await sendMessage(message);
            loadingBubble.remove();
            appendMessage(data.reply || fallbackMessage, false, true);
            if (data.handoffRequired && data.handoffUrl) {
                appendActionButton(data.handoffUrl, handoffButtonLabel);
            }
        } catch (error) {
            loadingBubble.remove();
            appendMessage(errorMessage, false, true);
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

    restoreHistoryIfAvailable();
    resetInactivityTimer();
})();
