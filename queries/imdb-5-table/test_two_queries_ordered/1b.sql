SELECT MIN(mc.note), MIN(t.title), MIN(t.production_year) FROM movie_companies AS mc INNER JOIN movie_info_idx AS mi_idx ON mc.movie_id = mi_idx.movie_id INNER JOIN title AS t ON t.id = mi_idx.movie_id INNER JOIN info_type AS it ON it.id = mi_idx.info_type_id INNER JOIN company_type AS ct ON ct.id = mc.company_type_id WHERE it.info = 'bottom 10 rank' AND mc.note NOT LIKE '%(as Metro-Goldwyn-Mayer Pictures)%' AND t.production_year BETWEEN 2005 AND 2010;
SELECT MIN(mc.note), MIN(t.title), MIN(t.production_year) FROM title AS t INNER JOIN movie_info_idx AS mi_idx ON t.id = mi_idx.movie_id INNER JOIN movie_companies AS mc ON mc.movie_id = mi_idx.movie_id INNER JOIN info_type AS it ON it.id = mi_idx.info_type_id INNER JOIN company_type AS ct ON ct.id = mc.company_type_id WHERE it.info = 'bottom 10 rank' AND mc.note NOT LIKE '%(as Metro-Goldwyn-Mayer Pictures)%' AND t.production_year BETWEEN 2005 AND 2010;
SELECT MIN(mc.note), MIN(t.title), MIN(t.production_year) FROM info_type AS it INNER JOIN movie_info_idx AS mi_idx ON it.id = mi_idx.info_type_id INNER JOIN title AS t ON t.id = mi_idx.movie_id INNER JOIN movie_companies AS mc ON mc.movie_id = mi_idx.movie_id INNER JOIN company_type AS ct ON ct.id = mc.company_type_id WHERE it.info = 'bottom 10 rank' AND mc.note NOT LIKE '%(as Metro-Goldwyn-Mayer Pictures)%' AND t.production_year BETWEEN 2005 AND 2010;
SELECT MIN(mc.note), MIN(t.title), MIN(t.production_year) FROM company_type AS ct INNER JOIN movie_companies AS mc ON ct.id = mc.company_type_id INNER JOIN movie_info_idx AS mi_idx ON mc.movie_id = mi_idx.movie_id INNER JOIN title AS t ON t.id = mi_idx.movie_id INNER JOIN info_type AS it ON it.id = mi_idx.info_type_id WHERE it.info = 'bottom 10 rank' AND mc.note NOT LIKE '%(as Metro-Goldwyn-Mayer Pictures)%' AND t.production_year BETWEEN 2005 AND 2010;
