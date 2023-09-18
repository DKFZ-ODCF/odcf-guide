UPDATE sample
SET species_with_strain = (
    SELECT regexp_replace(s.species_with_strain, '\)\[', ') [', 'g') /* added global flag for mixed-in species */
    FROM sample s
    WHERE sample.id = s.id
);
