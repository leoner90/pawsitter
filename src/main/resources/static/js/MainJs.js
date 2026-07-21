document.addEventListener("DOMContentLoaded", () =>
{
    const menuButton = document.querySelector(".mobile-menu-button");
    const navLinks = document.querySelector(".nav-links");

    if (!menuButton || !navLinks)
    {
        return;
    }

    menuButton.addEventListener("click", () =>
    {
        const isOpen = navLinks.classList.toggle("nav-links-open");

        menuButton.setAttribute("aria-expanded", String(isOpen));
        menuButton.textContent = isOpen ? "✕" : "☰";
    });
});