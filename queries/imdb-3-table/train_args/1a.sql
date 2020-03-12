SELECT MIN(t.title) FROM title AS t INNER JOIN movie_companies AS mc ON t.id = mc.movie_id INNER JOIN company_type AS ct ON ct.id = mc.company_type_id WHERE ct.kind = 'production companies';
SELECT MIN(t.title) FROM movie_companies AS mc INNER JOIN company_type AS ct ON ct.id = mc.company_type_id INNER JOIN title AS t ON t.id = mc.movie_id WHERE ct.kind = 'production companies';
