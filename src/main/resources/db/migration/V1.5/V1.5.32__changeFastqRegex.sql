UPDATE validation
SET regex = '^\/omics\/odcf\/transfer\/.+_[R|I][1|2]\.fastq\.gz$',
    description = 'Your given path does not match the required naming, please make sure to end with ''_R{1,2}.fastq.gz''<br>and starts with ''/omics/odcf/transfer/'''
WHERE field = 'fileName';
