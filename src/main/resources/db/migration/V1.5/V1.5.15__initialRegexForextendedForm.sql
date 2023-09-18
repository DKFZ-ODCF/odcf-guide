/* file */
INSERT into validation VALUES ('fileName', '^.+$', true, 'ERROR');
INSERT into validation VALUES ('md5', '^[a-f0-9]{32}$', true, 'ERROR');
INSERT into validation VALUES ('baseCount', '[0-9]*', false, 'ERROR');
INSERT into validation VALUES ('cycleCount', '[0-9]*', false, 'ERROR');

/* technical sample */
INSERT into validation VALUES ('readCount', '[0-9]*', false, 'ERROR');
UPDATE validation SET field = 'barcode' where field = 'technicalSampleBarcode';
UPDATE validation SET field = 'externalSubmissionId' where field = 'technicalSampleExternalSubmissionId';
UPDATE validation SET field = 'lane' where field = 'technicalSampleLane';

