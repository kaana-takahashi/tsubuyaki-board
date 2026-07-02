document.addEventListener("DOMContentLoaded", () => {
    const actionsList = Array.from(document.querySelectorAll(".post__actions"));

    function setOpen(actions, open) {
        const button = actions.querySelector(".post__menu-button");
        const menu = actions.querySelector(".post__menu");
        if (!button || !menu) {
            return;
        }
        menu.hidden = !open;
        button.setAttribute("aria-expanded", String(open));
    }

    function closeAll(except) {
        actionsList
            .filter(actions => actions !== except)
            .forEach(actions => setOpen(actions, false));
    }

    actionsList.forEach(actions => {
        const button = actions.querySelector(".post__menu-button");
        const menu = actions.querySelector(".post__menu");
        if (!button || !menu) {
            return;
        }

        button.addEventListener("click", event => {
            event.preventDefault();
            event.stopPropagation();
            const shouldOpen = menu.hidden;
            closeAll(actions);
            setOpen(actions, shouldOpen);
        });

        menu.addEventListener("click", event => {
            event.stopPropagation();
        });
    });

    document.addEventListener("click", event => {
        if (!event.target.closest(".post__actions")) {
            closeAll();
        }
    });

    document.addEventListener("keydown", event => {
        if (event.key === "Escape") {
            closeAll();
        }
    });
});
