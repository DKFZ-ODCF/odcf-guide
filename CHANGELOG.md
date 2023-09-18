# CHANGELOG


## 1.7.31


### Changes and Improvements

- GUIDE-729: added ability to make parser components optional (Timo Wiese @wieset)

## 1.7.30


### Changes and Improvements

- GUIDE-719: removed merging instructions from 'Submission has been validated' mail (Gregor Warsow @warsow)

### Bugfixes

- GUIDE-722: create a new ticket with REST API to receive a ticket number (Timo Wiese @wieset)
- GUIDE-725: added open to all scheduled tasks (Timo Wiese @wieset)

## 1.7.29


### Changes and Improvements

- GUIDE-363: added selection for sample type category (Oydin Iqbal @iqbalo)
- GUIDE-449: Implemented that unfinished external submissions will be removed after 90 days (Oydin Iqbal @iqbalo)
- GUIDE-718: added projects to transferred mail (Timo Wiese @wieset)

## 1.7.28


### New Features

- GUIDE-713: implemented a new API to be able to import submissions (Timo Wiese @wieset)

### Changes and Improvements

- GUIDE-709: changed species column's explanation text (Oydin Iqbal @iqbalo)
- GUIDE-712: forwarding to the overview with save and exit (Timo Wiese @wieset)

### Bugfixes

- GUIDE-711: added missing catch to handle not mappable column headers (Timo Wiese @wieset)
- GUIDE-717: fixed that users can complete submissions with requested values (Timo Wiese @wieset)

## 1.7.27


### New Features

- GUIDE-715: added the ability to build the guide into a docker container (Timo Wiese @wieset)

### Changes and Improvements

- GUIDE-700: added trigger endpoint to termination mail subject (Timo Wiese @wieset)

## 1.7.26


### Changes and Improvements

- GUIDE-411: created admin GUI for regex manipulation for the validation (Oydin Iqbal @iqbalo)
- GUIDE-705: changed cost center to organizational unit for queries to external metadata source (Timo Wiese @wieset)
- GUIDE-708: changed sample type input to convert all letters to lowercase (Timo Wiese @wieset)

## 1.7.25


### New Features

- GUIDE-668: implemented functionality that users can request SeqTypes (Oydin Iqbal @iqbalo)

### Changes and Improvements

- GUIDE-397: converted library preparation kit text field to dropdown field (Oydin Iqbal @iqbalo)

### Bugfixes

- GUIDE-694: set GUID bit for folders to which tsv files are written out (Gregor Warsow @warsow)
- GUIDE-703: fix xlsx empty rows bug (Gregor Warsow @warsow)

## 1.7.24


### New Features

- GUIDE-354: users can upload external submissions themselves (Timo Wiese @wieset)

### Changes and Improvements

- GUIDE-586: added new low_coverage_requested column in metadata (Oydin Iqbal @iqbalo)

### Bugfixes

- GUIDE-695: fixed wrong sample grouping on read only page (Timo Wiese @wieset)
- GUIDE-696: fixed wrong ONT paths in mails (Timo Wiese @wieset)
- GUIDE-697: Prevented loss of mails that encounter exceptions (Oydin Iqbal @iqbalo)
- GUIDE-699: fetch OE information while initializing Person object; respect accountDisabled status from LDAP (Gregor Warsow @warsow)

## 1.7.23


### Changes and Improvements

- GUIDE-680: added server side speed improvements and some refactoring (Timo Wiese @wieset)

### Bugfixes

- GUIDE-691: rows containing data but no fastq filename are no longer skipped, instead an exception is thrown (Timo Wiese @wieset)
- GUIDE-692: fixed error message if uploaded files have the wrong format or are not valid (Timo Wiese @wieset)

## 1.7.22


### Changes and Improvements

- GUIDE-623: made all mandatory metadata required in external data submission (Oydin Iqbal @iqbalo)
- GUIDE-677: receive organizational unit and organizational unit leader from external api (Timo Wiese @wieset)
- GUIDE-687: Send mail when submission was deleted (Oydin Iqbal @iqbalo)

### Bugfixes

- GUIDE-686: Fixed the deletion of extended submissions (Oydin Iqbal @iqbalo)
- GUIDE-690: fixed api responses against otp can be empty (Timo Wiese @wieset)

## 1.7.21


### Changes and Improvements

- GUIDE-548: combined instrument with sequencing kit (Timo Wiese @wieset)
- GUIDE-666: Added link to Project Config page in OTP (Timo Wiese @wieset & Oydin Iqbal @iqbalo)
- GUIDE-679: Added indicator whether files are readable on transfer (Timo Wiese @wieset)

## 1.7.20


### Changes and Improvements

- GUIDE-355: Added working name and comment for extended submissions (Oydin Iqbal @iqbalo)
- GUIDE-682: Changed comment field to only accept certain characters (Oydin Iqbal @iqbalo)
- GUIDE-685: Added field status to ilse import object (Timo Wiese @wieset)

## 1.7.19


### Bugfixes

- GUIDE-683: Do not write out the UUID column in the exported TSV (Timo Wiese @wieset)
- GUIDE-684: Do not export stopped samples (Timo Wiese @wieset)

## 1.7.18


### New Features

- GUIDE-648: implemented functionality that customers can request values (Oydin Iqbal @iqbalo)

### Changes and Improvements

- GUIDE-661: Created reset termination timer button (Oydin Iqbal @iqbalo)

### Bugfixes

- GUIDE-675: limited finished submission to the latest 100 (Timo Wiese @wieset)
- GUIDE-676: fixed UnsupportedOperationException while saving submission (Timo Wiese @wieset)
- GUIDE-678: Fixed bug OTP expects a space between species scientific name and strain (Oydin Iqbal @iqbalo)
- GUIDE-681: Fixed bug when requesting new species with mixed-in species (Oydin Iqbal @iqbalo)

## 1.7.17


### New Features

- GUIDE-674: improvements for cluster job feature (Timo Wiese @wieset)

### Changes and Improvements

- GUIDE-370: added reminder mails for external submissions (Timo Wiese @wieset)
- GUIDE-671: added ability to register a ticket number via API (Timo Wiese @wieset)

## 1.7.16


### New Features

- GUIDE-667: added ability to configure and run multiple dependent cluster jobs (Timo Wiese @wieset)

### Changes and Improvements

- GUIDE-573: Increase test coverage to 90% (Oydin Iqbal @iqbalo)
- GUIDE-673: increased json file size (Timo Wiese @wieset)

## 1.7.15


### New Features

- GUIDE-172: added cross guide submission merging check (Timo Wiese @wieset)
- GUIDE-647: added functionality to proceed with requested values (Timo Wiese @wieset)
- GUIDE-654: added possibility to run cluster jobs (Timo Wiese @wieset)
- GUIDE-672: Provide URL to fetch originally imported JSON (Gregor Warsow @warsow)

### Changes and Improvements

- GUIDE-665: removed unknown values from external data export (Timo Wiese @wieset)
- GUIDE-669: added ability to import and export different properties (Timo Wiese @wieset)

### Bugfixes

- GUIDE-670: fixed project prefixes from closed projects not being found (Timo Wiese @wieset)

### Other

- GUIDE-631: simplified and cleaned up merging candidates (Timo Wiese @wieset)
- GUIDE-656: introduce OTP adapter (Timo Wiese @wieset)

## 1.7.14


### Changes and Improvements

- GUIDE-660: Simplify getter for baseMaterial (Oydin Iqbal @iqbalo)
- GUIDE-664: new mail layout (Timo Wiese @wieset)

### Bugfixes

- GUIDE-662: fixed that new samples could not have more than one species (Timo Wiese @wieset)
- GUIDE-663: Change toString method of sequencing type (Oydin Iqbal @iqbalo)

## 1.7.13


### New Features

- GUIDE-646: Create table and GUI for newly requested values (Oydin Iqbal @iqbalo)
- GUIDE-657: added possibility to mark the news link with red dot (Tim Lorbacher @t926r)

### Changes and Improvements

- GUIDE-568: merge columns highlighted (Timo Wiese @wieset)

## 1.7.12


### Changes and Improvements

- GUIDE-563: Add project form link if no project is selected (Oydin Iqbal @iqbalo)
- GUIDE-640: Introduce logfile for mails and shorten the main logfile (Oydin Iqbal @iqbalo)

### Bugfixes

- GUIDE-659: fixed csv import bug (Timo Wiese @wieset)

## 1.7.11


### New Features

- GUIDE-555: Show old/current parser upon editing (Tim Lorbacher @t926r)

### Changes and Improvements

- GUIDE-137: Check whether the selected project contains human or mouse data (Oydin Iqbal @iqbalo)
- GUIDE-527: adapted sorting on read only page (Timo Wiese @wieset)
- GUIDE-622: Improved CSV import (Oydin Iqbal @iqbalo)
- GUIDE-625: authorization for page providing users to be notified for given project is now possible via token (Gregor Warsow @warsow)
- GUIDE-652: Updated information texts for validation columns (Oydin Iqbal @iqbalo)
- GUIDE-655: do not send empty feedback mails (Timo Wiese @wieset)

## 1.7.10


### New Features

- GUIDE-643: implemented validation level to gui and server-side validation (Timo Wiese @wieset)
- GUIDE-644: added entity cluster job (Oydin Iqbal @iqbalo)

### Other

- GUIDE-572: increase test coverage to 80 percent (Timo Wiese @wieset)

## 1.7.9


### Changes and Improvements

- GUIDE-653: made project path accessible via token (Tim Lorbacher @t926r)

### Bugfixes

- GUIDE-651: Do not send empty on-hold reminder mail (Oydin Iqbal @iqbalo)

## 1.7.8


### New Features

- GUIDE-621: added validation level and sequencing technology to submission (Timo Wiese @wieset)

### Changes and Improvements

- GUIDE-599: Added information texts, mouse overs and error messages for parser feature (Oydin Iqbal @iqbalo)
- GUIDE-602: Make parser available for ILSe submission (Oydin Iqbal @iqbalo)
- GUIDE-645: adapted import and made it more robust (Timo Wiese @wieset)
- GUIDE-649: Include link to news page in header (Oydin Iqbal @iqbalo)

## 1.7.7


### Changes and Improvements

- GUIDE-626: Added reminder mail when submission is on hold for more than 48h (Oydin Iqbal @iqbalo)

### Other

- GUIDE-630: cleanup backend functions (Timo Wiese @wieset)
- GUIDE-634: creation of tests for metadata merging (Timo Wiese @wieset)

## 1.7.6


### New Features

- GUIDE-598: terminate open submissions 90 days after external data availability (Timo Wiese @wieset)

### Bugfixes

- GUIDE-635: fixed merging warning with stopped samples (Timo Wiese @wieset)

### Other

- GUIDE-571: increase test coverage to 70 percent (Timo Wiese @wieset)

## 1.7.5


### New Features

- GUIDE-619: added entity sequencing technology (Timo Wiese @wieset)

### Changes and Improvements

- GUIDE-557: projects not registered in otp will no longer be imported (Timo Wiese @wieset)
- GUIDE-613: improved loading speed of details pages and change of the dropdowns appearance (Timo Wiese @wieset)
- GUIDE-628: added ability to add sequencing types without displaying them (Timo Wiese @wieset)

### Bugfixes

- GUIDE-616: fixed problems after releasing external data feature (Timo Wiese @wieset)

### Other

- GUIDE-173: excluded internal columns from export (Timo Wiese @wieset)
- GUIDE-540: use Spring 5 WebClient instead of BufferedReader to read from ilse api (Timo Wiese @wieset)
- GUIDE-618: get rid of log messages for anonymous user (Timo Wiese @wieset)

## 1.7.4


### Changes and Improvements

- GUIDE-614: Added code documentation for Services (Oydin Iqbal @iqbalo)
- GUIDE-627: provide content of submission received mail for autoclosed submissions as appendix in FinallySubmittedMail (Gregor Warsow @warsow)

## 1.7.3


### New Features

- GUIDE-604: make ILSe Api accessible via token (Gregor Warsow @warsow)

### Changes and Improvements

- GUIDE-608: make Fasttrack known to Guide (Tim Lorbacher @t926r)
- GUIDE-610: Inform Operator about Fasttrack Submissions (Tim Lorbacher @t926r)
- GUIDE-615: simplify originProjectsSet (Tim Lorbacher @t926r)
- GUIDE-629: do not set submission on hold if species is 'OTHER' (Gregor Warsow @warsow)

## 1.7.2


### New Features

- GUIDE-620: added entity validation level (Timo Wiese @wieset)

### Changes and Improvements

- GUIDE-608: make Fasttrack known to Guide (Tim Lorbacher @t926r)

### Bugfixes

- GUIDE-591: fixed server validation for maximum length of PID (Gregor Warsow @warsow)

## 1.7.1


### Changes and Improvements

- GUIDE-522: base_material column for export (Tim Lorbacher @t926r)

### Bugfixes

- GUIDE-617: fixed broken user overview (Timo Wiese @wieset)

## 1.7.0


### New Features

- GUIDE-310: registration mail for uploaded submission (Tim Lorbacher @t926r)
- GUIDE-489: implemented the feature to add or remove lines on the extended page (Timo Wiese @wieset)
- GUIDE-503: New Page for external Feature (Timo Wiese @wieset)
- GUIDE-545: fixed 'back and edit' button and otp merged samples on external data feature (Timo Wiese @wieset)
- GUIDE-546: adapted validation regexes and descriptions (Timo Wiese @wieset)

### Changes and Improvements

- GUIDE-425: Implemented unvalidated deletion of submissions and catching of duplicated md5-sums at TSV-Upload (Oydin Iqbal @iqbalo)
- GUIDE-606: disabled json extraction for auto-closed submissions (Gregor Warsow @warsow)
- GUIDE-611: Make the sending of mails asynchronous (Oydin Iqbal @iqbalo)

### Bugfixes

- GUIDE-600: do not use empty PID for detection of similar PIDs; also show PID and sampleType in found-similar-pid email (Gregor Warsow @warsow)
- GUIDE-612: improved loading speed of statistic page (Timo Wiese @wieset)

### Other

- GUIDE-609: fixed javascript problems and introduced eslint (Timo Wiese @wieset)

## 1.6.14


### New Features

- GUIDE-368: added species to edit pages (Tim Lorbacher @t926r & Oydin Iqbal @iqbalo & Timo Wiese @wieset)
- GUIDE-565: import species into guide (Tim Lorbacher @t926r & Oydin Iqbal @iqbalo & Timo Wiese @wieset)

### Changes and Improvements

- GUIDE-537: excluded withdrawn samples from the merging warning (Oydin Iqbal @iqbalo)
- GUIDE-601: created limited pool connections to external databases (Timo Wiese @wieset)

### Bugfixes

- GUIDE-605: fixed the ON_HOLD state setting the lock user (Timo Wiese @wieset)

## 1.6.13


### Changes and Improvements

- GUIDE-585: added new on hold state (Timo Wiese @wieset , Oydin Iqbal @iqbalo , Tim Lorbacher @t926r)
- GUIDE-603: make tomcat's maxParameterCount adaptable and add properties check on startup (Timo Wiese @wieset)

## 1.6.12


### Changes and Improvements

- GUIDE-206: added storage overview for organizational unit (Tim Lorbacher @t926r)

### Bugfixes

- GUIDE-595: empty SeqType is regarded as a valid value in client-side validation (Gregor Warsow @warsow)

## 1.6.11


### New Features

- GUIDE-569: send mail if there are similar PIDs in different projects (Timo Wiese @wieset)

### Changes and Improvements

- GUIDE-531: perform json extractor script for auto closed submission (Timo Wiese @wieset)
- GUIDE-554: added delete button for last Parser Field Component (Oydin Iqbal @iqbalo)
- GUIDE-558: added delete function for Parser (Oydin Iqbal @iqbalo)

### Bugfixes

- GUIDE-590: use single cell from sequencing type instead of sample (Timo Wiese @wieset)

## 1.6.10


### Changes and Improvements

- GUIDE-577: Changed path from /icgc/dkfzlsdf/ to /omics/odcf/ in runJsonExtractorScript (Oydin Iqbal @iqbalo)
- GUIDE-587: always show back&edit button when the submission is active (Tim Lorbacher @t926r)

### Bugfixes

- GUIDE-582: removed unessesary empty row at the end of tsv file (Tim Lorbacher @t926r)
- GUIDE-584: fixed project selection while uploading tsv by admins (Timo Wiese @wieset)
- GUIDE-594: Fixed read-only-page if no sequencing type is given (Oydin Iqbal @iqbalo)
- GUIDE-596: fixed error on details page if server side validation failed (Timo Wiese @wieset)

## 1.6.9


### Changes and Improvements

- GUIDE-478: get storage sizes from isilon database (Timo Wiese @wieset)
- GUIDE-588: use correct subject for json extraction / merging mails to ticket system (Gregor Warsow @warsow)

## 1.6.8


### Changes and Improvements

- GUIDE-580: added average feedback for feedback overview (Tim Lorbacher @t926r)

## 1.6.6


### Changes and Improvements

- GUIDE-501: handle proceed stop flag from GPCF (Timo Wiese @wieset)
- GUIDE-578: send mail when submission is terminated (Oydin Iqbal @iqbalo)

### Other

- GUIDE-515: added initial admin statistics (Timo Wiese @wieset)
- GUIDE-564: adapted otp query to handle new project fields structure (Timo Wiese @wieset)

## 1.6.5


### Changes and Improvements

- GUIDE-499: adapted summary page so that it hides unnecessary columns (Timo Wiese @wieset)

### Bugfixes

- GUIDE-561: fixed reset of empty submissions (Gregor Warsow @warsow)
- GUIDE-574: fixed reminder mails and overview for new unlocked state (Timo Wiese @wieset)

## 1.6.4


### Changes and Improvements

- GUIDE-482: trigger table merging after setting data received via GUI (Timo Wiese @wieset)

### Bugfixes

- GUIDE-568: fixed response if mails cannot send while importing (Timo Wiese @wieset)
- GUIDE-575: fix final submit mail for auto closed submissions (Timo Wiese @wieset)

## 1.6.3


### Changes and Improvements

- GUIDE-509: prevented that admins and users can write data at the same time (Timo Wiese @wieset)
- GUIDE-562: Added final submit user to finally-submitted-mail (Oydin Iqbal @iqbalo)

### Other

- GUIDE-567: renamed reset button for ilse submissions (Timo Wiese @wieset)

## 1.6.2


### Changes and Improvements

- GUIDE-486: improved submission overview loading time and made it asynchronous (Timo Wiese @wieset)

## 1.6.1


### New Features

- GUIDE-427: implemented the possibility to import from midterm (Timo Wiese @wieset)

### Changes and Improvements

- GUIDE-502: added imprint to footer (Oydin Iqbal @iqbalo)
- GUIDE-559: adapt link to ABT registration in OTP (Oydin Iqbal @iqbalo)

### Bugfixes

- GUIDE-560: prevented injection of illegal projects via tsv upload (Timo Wiese @wieset)

## 1.6.0


### New Features

- GUIDE-365: Parser in Guide (Oydin Iqbal @iqbalo)
- GUIDE-446: Add parseIdentifier to Sample (Oydin Iqbal @iqbalo)
- GUIDE-514: Add parser tables to the database (Oydin Iqbal @iqbalo)
- GUIDE-516: Parser Overview for Admins (Oydin Iqbal @iqbalo)
- GUIDE-517: Adding of Information Page for Parsers (Oydin Iqbal @iqbalo)
- GUIDE-518: GUI page for admins to add a new Parser (Oydin Iqbal @iqbalo)
- GUIDE-519: GUI page for admins to edit a Parser (Oydin Iqbal @iqbalo)
- GUIDE-520: Add functionality to apply parser on a submission (Oydin Iqbal @iqbalo)

## 1.5.40


### Changes and Improvements

- GUIDE-551: added project import aliases to unknown project detection (Timo Wiese @wieset)

## 1.5.39


### Bugfixes

- GUIDE-549: Handle saving of Index Type correctly (Oydin Iqbal @iqbalo)

## 1.5.38


### New Features

- GUIDE-402: implemented check if sample data from guide and gpcf has inconsistency (Timo Wiese @wieset)
- GUIDE-403: expanded fastq file name to absolute path (Timo Wiese @wieset)

### Changes and Improvements

- GUIDE-529: updated expected path to new path logic and fixed antibody target issue (Timo Wiese @wieset)

### Bugfixes

- GUIDE-525: do not show xenograft value for sample data from OTP (Timo Wiese @wieset)
- GUIDE-544: Handle empty sequencing type on the read only page correctly (Oydin Iqbal @iqbalo)

## 1.5.37


### Bugfixes

- GUIDE-543: Bug fix - Finally Submitted page was shown even when the submission was not finished yet (Oydin Iqbal @iqbalo)

## 1.5.36


### Changes and Improvements

- GUIDE-456: changed fastq file name to fastq file path (Timo Wiese @wieset)

### Bugfixes

- GUIDE-534: Prevented reopening a submission by clicking back and edit (Timo Wiese @wieset)
- GUIDE-539: Preventing the final submit of a submission in another state than VALIDATED (Timo Wiese @wieset)

## 1.5.35


### Bugfixes

- GUIDE-538: Fixed that some ilse api data was lost when saving via the gui (Timo Wiese @wieset)

## 1.5.34


### Bugfixes

- GUIDE-506: Merging Warning for external feature (Oydin Iqbal @iqbalo)

## 1.5.33


### Changes and Improvements

- GUIDE-500: Split submission into subclasses (Oydin Iqbal @iqbalo)
- GUIDE-528: Highlight columns on read-only page (Timo Wiese @wieset)
- GUIDE-536: Add 'proceed' from ILSe Api to known fields (Timo Wiese @wieset)

### Bugfixes

- GUIDE-524: disabled user sorting on the summary page (Timo Wiese @wieset)
- GUIDE-532: fixed bug which showed an error page after resetting a submission although reset ran through correctly (Gregor Warsow @warsow)

## 1.5.32


### Bugfixes

- GUIDE-523: merging warning did not work for xenograft samples (Gregor Warsow @warsow)

## 1.5.31


### Other

- added more documentation (Timo Wiese @wieset)

## 1.5.30


### New Features

- GUIDE-428: export and import function for extended table (Timo Wiese @wieset)

### Bugfixes

- GUIDE-330: it is now ensured that all ssh errors are reported correctly (Timo Wiese @wieset)

## 1.5.29


### Changes and Improvements

- GUIDE-465: added regex to fastq filename (Timo Wiese @wieset)
- GUIDE-487: Improvement of merging warning (Oydin Iqbal @iqbalo)
- GUIDE-479: added read lengths from ilse api and use them to decide if paired or single (Timo Wiese @wieset)

### Bugfixes

- GUIDE-504: all non ILSe submissions are now unresettable (Timo Wiese @wieset)
- GUIDE-512: reminder mails were not sent out (Gregor Warsow @warsow)

### Other

- added code style tool ktlint (Timo Wiese @wieset)

## 1.5.28


### Bugfixes

- GUIDE-511: make metadata folder group-writable (Gregor Warsow @warsow, Timo Wiese @wieset)

## 1.5.27


### Changes and Improvements

- GUIDE-237: Add GPCF Name(s) to SeqType (Oydin Iqbal @iqbalo)
- GUIDE-472: write out tsv to core filestructure (Gregor Warsow @warsow)

### Bugfixes

- GUIDE-505: handled invalide mail adresses and send the mail to all other recipients (Timo Wiese @wieset)
- fixed error page to display code and message (Timo Wiese @wieset)

## 1.5.26


### New Features

- GUIDE-179: added data security statement (Timo Wiese @wieset)

### Changes and Improvements

- GUIDE-396: LibPrepKit and IndexType not editable for "samples" (Oydin Iqbal @iqbalo)
- GUIDE-421: Added Guide User for submitters without AD-account (Oydin Iqbal @iqbalo)
- GUIDE-450: Adapt Index Type for usage (Oydin Iqbal @iqbalo)
- GUIDE-451: md5 sum is now the anchor for files and no longer the name (Timo Wiese @wieset)
- GUIDE-469:  increase test coverage to >40% (Timo Wiese @wieset)
- GUIDE-477: Implement API to Isilon (Oydin Iqbal @iqbalo)
- GUIDE-481: disabled regex check for reset, terminate and finished externally buttons (Timo Wiese @wieset)

### Other

- GUIDE-443: added new logo (Timo Wiese @wieset)
- introduce auto generation of changelog
