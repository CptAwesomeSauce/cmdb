SELECT * FROM review;
SELECT * FROM user;
SELECT * FROM movie WHERE title = 'Up';
SELECT * FROM movie_user;

DELETE FROM movie
WHERE title = 'Up' AND genre = 'Adventure';

SELECT COUNT(*)
FROM movie_user
WHERE ISAN_ID = 3322;

1st
INSERT into movie_user
values isan = ? and userID = ?

2nd
COUNT(distinct(names))
FROM movie_user
where isan = ?

