ALTER TABLE validation ADD COLUMN description VARCHAR(255);

UPDATE validation SET description='choose element from list' WHERE regex = 'dropdown';

-- ^[A-Za-z0-9_+-]{3,42}$
UPDATE validation SET description='alphanumerical characters, minus/underscore allowed, length between 3 and 42;<br>no whitespaces allowed'
WHERE field = 'pid';

-- ^[a-z0-9+-]{1,}[0-9]+$
UPDATE validation SET description='lowercase alphanumerical characters, plus/minus allowed; must end with a digit; minimum length: 2;<br>Do not encode the antibody target. It will be added automatically.'
WHERE field = 'sampleType';
UPDATE validation SET regex='^[a-z0-9+-]+[0-9]+$'
WHERE field = 'sampleType';

-- ^[a-z0-9+-]+$
UPDATE validation SET description='lowercase alphanumerical characters, plus/minus allowed; minimum length: 2;<br>Do not encode the antibody target. It will be added automatically.'
WHERE field = 'oldSampleType';
UPDATE validation SET regex='^[a-z0-9+-]{2,}$'
WHERE field = 'oldSampleType';

-- ^[a-zA-Z0-9]*$
UPDATE validation SET description='alphanumerical characters; no special characters (e.g. +/-/_ etc.) allowed;<br>no whitespaces allowed;<br>can be left empty'
WHERE field = 'singleCellWellPosition';

-- ^[a-zA-Z0-9]*$
UPDATE validation SET description='alphanumerical characters; no special characters (e.g. +/-/_ etc.) allowed;<br>no whitespaces allowed;<br>can be left empty'
WHERE field = 'singleCellPlate';

-- ^\d{1,3}$
UPDATE validation SET description='Number between 1 and 999'
WHERE field = 'tagmentationLibrary';

INSERT into validation VALUES ('technicalSampleBarcode', '^([AGTC]{6,8})(-(?=([AGTC]{6,8}))([AGTC]{6,8}))?$', false, 'One or two blocks of DNA sequence;<br>The blocks have 6-8 bases; blocks separated by hyphen.');
INSERT into validation VALUES ('technicalSampleExternalSubmissionId', '\d{6}', false, 'Number with 6 digits');
INSERT into validation VALUES ('technicalSampleLane', '[1-8]?', false, 'Number between 1-8; can be left empty');
