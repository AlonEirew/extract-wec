DROP TABLE IF EXISTS MENTIONS;
DROP TABLE IF EXISTS COREFS;
DROP TABLE IF EXISTS CONTEXTS;

CREATE TABLE MENTIONS (
    mention_id INT AUTO_INCREMENT PRIMARY KEY,
    coref_id INT NOT NULL,
    context_id INT NOT NULL,
    mention_text VARCHAR(250) NOT NULL,
    token_start INT NOT NULL,
    token_end INT NOT NULL,
    extracted_from_page VARCHAR(250) NOT NULL
);

CREATE TABLE COREFS (
    coref_id INT AUTO_INCREMENT PRIMARY KEY,
    coref_value VARCHAR(250) NOT NULL,
    coref_type VARCHAR(250) DEFAULT NULL,
    coref_sub_type VARCHAR(250) DEFAULT NULL
);

CREATE TABLE CONTEXTS (
    context_id INT AUTO_INCREMENT PRIMARY KEY,
    context TEXT NOT NULL
);
