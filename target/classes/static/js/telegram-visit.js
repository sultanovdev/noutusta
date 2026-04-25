(() => {
    const endpoint = "/api/telegram/visit";

    function buildPayload(anchor) {
        return {
            source: anchor.dataset.telegramSource || "telegram_link",
            target: anchor.href || "",
            page: window.location.pathname || "",
            referrer: document.referrer || "",
            lang: document.documentElement.lang || navigator.language || ""
        };
    }

    function reportVisit(anchor) {
        const payload = buildPayload(anchor);
        const body = JSON.stringify(payload);
        const blob = new Blob([body], {type: "application/json"});

        if (navigator.sendBeacon) {
            navigator.sendBeacon(endpoint, blob);
            return;
        }

        fetch(endpoint, {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body,
            keepalive: true
        }).catch(() => {
            // Ignore telemetry failures to keep the click flow smooth.
        });
    }

    document.addEventListener("click", (event) => {
        const anchor = event.target.closest("a[data-telegram-track='true']");
        if (!anchor) {
            return;
        }
        reportVisit(anchor);
    });
})();
