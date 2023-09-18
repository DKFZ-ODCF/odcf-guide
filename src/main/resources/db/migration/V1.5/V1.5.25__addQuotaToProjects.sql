ALTER TABLE otp_cached_project ADD COLUMN quota_project_folder BIGINT;
ALTER TABLE otp_cached_project ADD COLUMN quota_analysis_folder BIGINT;

UPDATE otp_cached_project SET quota_project_folder = -1 where quota_project_folder IS NULL;
UPDATE otp_cached_project SET quota_analysis_folder = -1 where quota_analysis_folder IS NULL;

INSERT INTO runtime_options VALUES ('projectFolderQuota', '10000'); /*10 PB in TB*/
INSERT INTO runtime_options VALUES ('analysisFolderQuota', '10000'); /*10 PB in TB*/

CREATE OR REPLACE FUNCTION random_string(length integer) RETURNS TEXT AS
$$
declare
    chars text[] := '{0,1,2,3,4,5,6,7,8,9,A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W,X,Y,Z,a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z}';
    result text := '';
    i integer := 0;
begin
    if length < 0 then
        raise exception 'Given length cannot be less than 0';
    end if;
    for i in 1..length loop
            result := result || chars[1+random()*(array_length(chars, 1)-1)];
        end loop;
    return result;
end;
$$ language plpgsql;
SELECT random_string(32);

ALTER TABLE person ADD COLUMN api_token VARCHAR(32);
UPDATE person SET api_token = random_string(32) WHERE api_token IS NULL;
ALTER TABLE person ALTER COLUMN api_token SET NOT NULL;
