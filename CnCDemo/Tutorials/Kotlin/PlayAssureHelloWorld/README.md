Tutorial - PlayAssure HelloWorld Sample
=======================================
## Summary
A simple HelloWorld getting started example for how to use Penthera Play Assure.

#### Steps to use the SDK:
* Add the Penthera maven repository to your project
* Create the PlayAssureManager object by providing the url for the asset to be streamed and a few simple configuration options.
* Provide registration details using the startup method on first use.
* Once the manager has initialized it will call back an observer to notify that it is ready for playback.
* Request the playback url from the manager, this will be an http://localhost address and requires your application security to permit non-ssl local connections.
* Pass the playback url to your player and treat all other aspects of playback as normal.



