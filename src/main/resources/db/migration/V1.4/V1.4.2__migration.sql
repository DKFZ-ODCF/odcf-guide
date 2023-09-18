ALTER TABLE submission ADD import_date timestamp;

UPDATE submission SET status = 'AUTO_CLOSED', closed_user = null, closed_date = null WHERE closed_user = 'ODCF-Guide auto close';