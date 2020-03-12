SELECT MIN(cn.name) AS name FROM movie_companies AS mc INNER JOIN movie_keyword AS mk ON mc.movie_id = mk.movie_id INNER JOIN company_name AS cn ON cn.id = mc.company_id WHERE cn.country_code = '[us]';
SELECT MIN(cn.name) AS name FROM company_name AS cn INNER JOIN movie_companies AS mc ON cn.id = mc.company_id INNER JOIN movie_keyword AS mk ON mc.movie_id = mk.movie_id WHERE cn.country_code = '[us]';

