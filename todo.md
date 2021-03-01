Features:
1. fill in pictures
1. import words from a CSV file
1. mark the word as added in Dynalist (return a list of added words and pass it to a marking service?)
1. use the $word from an Oxford reply (can differ from the requested word)?
1. parse subsenses from the Oxford Dictionary definitions?
1. fill in audio (WaveNet via Google Cloud)?

Bugs:
1. Anki is not created when a note with an equal front already exists.
  * Merge?
  * Add a discriminator?
  * Do not add discriminators if only one definition?

Tech debt:
1. make the destination Anki deck name and model name configurable
1. specify the required Anki model fields in the README
1. create client libraries for external systems (Dynalist, Oxford Dictionary, Anki) as separate projects