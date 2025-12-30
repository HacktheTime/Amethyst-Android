You can get the Profiles as well as the Accounts the User has via an Intent at LaunchIntentDataProviderService

You can then launch the Profile and Account using an Intent at the StartMinecraftService

if the account is not set it will launch the current user. if you provide non or current it will default to current. You may use a mcuuid or a username. Case does not matter. UUIDs may be without -

if the profile is set to current or not set the current is assumed.



Tipp:
You can let the User Install a Mod and communicate with Minecraft using a Socket. With that connection you can configure something that will allow you to do something as soon as the game is started. Do not forget to make the Socket Localhost only!

For an example see (MISSING INSERT HYPE). Keep in mind that this external Resource is not part of the project and thereby not secured or supported by us. USE AT YOUR OWN RISK!