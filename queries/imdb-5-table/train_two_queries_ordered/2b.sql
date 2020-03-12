SELECT MIN(t.title) AS movie_title FROM movie_keyword AS mk INNER JOIN movie_companies AS mc ON mc.movie_id = mk.movie_id INNER JOIN company_name AS cn ON cn.id = mc.company_id INNER JOIN title AS t ON t.id = mk.movie_id INNER JOIN keyword AS k ON k.id = mk.keyword_id WHERE cn.country_code = '[nl]' AND k.keyword = 'character-name-in-title';
SELECT MIN(t.title) AS movie_title FROM movie_companies AS mc INNER JOIN movie_keyword AS mk ON mc.movie_id = mk.movie_id INNER JOIN keyword AS k ON k.id = mk.keyword_id INNER JOIN title AS t ON t.id = mk.movie_id INNER JOIN company_name AS cn ON cn.id = mc.company_id WHERE cn.country_code = '[nl]' AND k.keyword = 'character-name-in-title';
SELECT MIN(t.title) AS movie_title FROM title AS t INNER JOIN movie_keyword AS mk ON t.id = mk.movie_id INNER JOIN movie_companies AS mc ON mc.movie_id = mk.movie_id INNER JOIN keyword AS k ON k.id = mk.keyword_id INNER JOIN company_name AS cn ON cn.id = mc.company_id WHERE cn.country_code = '[nl]' AND k.keyword = 'character-name-in-title';
SELECT MIN(t.title) AS movie_title FROM company_name AS cn INNER JOIN movie_companies AS mc ON cn.id = mc.company_id INNER JOIN title AS t ON mc.movie_id = t.id INNER JOIN movie_keyword AS mk ON t.id = mk.movie_id INNER JOIN keyword AS k ON mk.keyword_id = k.id WHERE cn.country_code = '[nl]' AND k.keyword = 'character-name-in-title';



