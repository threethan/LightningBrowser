function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

function myFunction() {
    document.location = "https://open.spotify.com/collection/tracks";
}

async function delay() {
    for (let i = 1; i < 6; i++) {
        await sleep(1000);
        var lib = document.querySelector("body.mobile-web-player #main div div div div div a:nth-child(3)");
        if (document.location == "https://open.spotify.com/collection/tracks") lib.click();
        else lib.addEventListener("click", myFunction);
    }
    var lib = document.querySelector("body.mobile-web-player #main div div div div div a:nth-child(3)");
    lib.addEventListener("click", myFunction);
}
delay();