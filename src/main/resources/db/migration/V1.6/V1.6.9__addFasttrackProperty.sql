ALTER TABLE submission ADD fasttrack BOOLEAN;

UPDATE submission SET fasttrack = false WHERE submission.fasttrack IS NULL;
