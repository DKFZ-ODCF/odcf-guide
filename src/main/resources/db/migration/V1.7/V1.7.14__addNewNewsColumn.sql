ALTER TABLE person ADD unread_news BOOLEAN;

UPDATE person SET unread_news = false WHERE person.unread_news IS NULL;
