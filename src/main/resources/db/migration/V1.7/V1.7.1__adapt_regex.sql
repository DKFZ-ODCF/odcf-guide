UPDATE validation
SET description='Number with 4 - 6 digits'
WHERE regex = 'externalSubmissionId';

UPDATE validation
SET regex='^([AGTCN]{6,10})(-(?=([AGTCN]{6,10}))([AGTCN]{6,10}))?$',
    description='One or two blocks of DNA sequence;<br>The blocks have 6-10 bases; blocks separated by hyphen.'
WHERE regex = 'barcode';
