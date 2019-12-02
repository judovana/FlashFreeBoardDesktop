# FlashFreeBoardDesktop
Implementation of moonboard-like approach, on custom indoor climbing walls - PC client. See [video](http://www.facebook.com/1403584339870419/videos/562209057939918/)

This is fully moonbaord leds compatible client, however, due to no-limits on leds shining, I strongly recomends to set up own power - eg on each 100leds, you should have one [5v 10a source](https://www.mouser.com/ProductDetail/mean-well/rs-50-5/?qs=pqZ7J9Gt%2FmqSEKuHhGNBSg==&countrycode=CZ&currencycode=CZK).  See also http://www.eerkmans.nl/powering-lots-of-leds-from-arduino/ for some info.

The tooling contains one [arduino](https://www.sparkfun.com/products/13975) to [control](https://github.com/judovana/arduino/tree/master/FreeBoard/WS2812blinks) the leds. Note that arduino with its 2kb of ram have limitations around 600 of leds. For bigger walls you need arduino mega.
Second in chain is pc or raspery with mouse and screen or jsut touch screen. The links to older raspbery images are to be found in release.

Communication between raspbery/pc and arduino is via USB cable (up to 100m) or through blkuetoth, eg https://www.sparkfun.com/products/12580 , however the BT have some glitches, so cable is 1005 recomended.

Except HW, you have no limitations on wall you build. Any holds, any size. Just mak ea picture of it and import it as new wall, and start doing boulders on ot. The client of yours can be connected to any git repository to store boulders also on remote
 - backup
 - remote maintance
 - several walls with same holds
 - several different walls on same repo
However, there do not - intentionaly - exists shared world as on moonboard. well averybody is about to have custom layout :)

You can set it up also as helper on campus like devices, to highlight some parts of it.
If your training wall is small or moon-leds to expenisve, you can easily set your own matrix via eg those [leds](https://www.sparkfun.com/products/12877) - verifed, I have my home wall and am developing this app on those.

As written above, there is no limitation on numner of leds or even on number of lightened-on leds, however you powersource is limited, and leds eat - a bit only, but we have a lot of them. So there is built in protection to under-power. The leds will get dimmer from some point. Set upthis protection carefully!

**Although the code is open, this is not entirely free stuff! Contact us by rising issue her, or by contacting http://www.flashbb.cz/**. **You are allowed to use this for non-comercial usage, to the size of  moonboaard  - 11x18 max.** Any comercial or any bigger aray of leds in any dimesnion, must be consulted.  Wall around 20x20 leds, delivered out of the box, with holds set for your difficulty, is priced aprox 20000eur, without transfer fees.

Long live great moonboard, but from some time it starts to get boring, and without posibility to set up custom holds (which kills its meaning) it is doomed.
