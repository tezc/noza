;             
CREATE USER IF NOT EXISTS NOZA SALT '24cb7150db5d1480' HASH '24e627090fb6b47bd9894525453e6ebf09a12a50c6068183501f2ead653438db' ADMIN;         
CREATE SCHEMA IF NOT EXISTS BROKER AUTHORIZATION NOZA;        
CREATE SCHEMA IF NOT EXISTS CLIENT AUTHORIZATION NOZA;        
CREATE CACHED TABLE BROKER.MSGS(
    MSG_ID CHAR(36) NOT NULL,
    PACKET_ID INT DEFAULT NULL,
    TOPIC TEXT,
    QOS INT DEFAULT NULL,
    DATA BLOB,
    RETAIN TINYINT DEFAULT NULL,
    TIMESTAMP DATETIME NOT NULL
);           
ALTER TABLE BROKER.MSGS ADD CONSTRAINT BROKER.CONSTRAINT_2 PRIMARY KEY(MSG_ID);               
-- 0 +/- SELECT COUNT(*) FROM BROKER.MSGS;    
CREATE CACHED TABLE BROKER.CLIENTS(
    CLIENT_ID VARCHAR(128) NOT NULL SELECTIVITY 100,
    STATE TINYINT DEFAULT NULL SELECTIVITY 50,
    CLEAN_SESSION TINYINT DEFAULT NULL SELECTIVITY 50,
    WILL_MSG CHAR(36) DEFAULT NULL SELECTIVITY 50,
    LOCAL_ADDRESS VARCHAR(32) DEFAULT NULL SELECTIVITY 25,
    REMOTE_ADDRESS VARCHAR(32) DEFAULT NULL SELECTIVITY 25,
    FIRST_TIMESTAMP DATETIME DEFAULT CURRENT_TIMESTAMP() SELECTIVITY 100,
    LATEST_TIMESTAMP DATETIME NOT NULL SELECTIVITY 100
);         
ALTER TABLE BROKER.CLIENTS ADD CONSTRAINT BROKER.CONSTRAINT_5 PRIMARY KEY(CLIENT_ID);         
-- 0 +/- SELECT COUNT(*) FROM BROKER.CLIENTS; 
CREATE CACHED TABLE BROKER.CLIENT_IN_MSGS(
    CLIENT_ID VARCHAR(128) NOT NULL,
    MSG_ID CHAR(36) NOT NULL,
    PACKET_ID SMALLINT DEFAULT NULL,
    STATE TINYINT DEFAULT NULL,
    TIMESTAMP DATETIME NOT NULL
);   
ALTER TABLE BROKER.CLIENT_IN_MSGS ADD CONSTRAINT BROKER.CONSTRAINT_8 PRIMARY KEY(CLIENT_ID, MSG_ID);          
-- 0 +/- SELECT COUNT(*) FROM BROKER.CLIENT_IN_MSGS;          
CREATE CACHED TABLE BROKER.CLIENT_OUT_MSGS(
    CLIENT_ID VARCHAR(128) NOT NULL,
    MSG_ID CHAR(36) NOT NULL,
    PACKET_ID SMALLINT DEFAULT NULL,
    STATE TINYINT DEFAULT NULL,
    TIMESTAMP DATETIME DEFAULT CURRENT_TIMESTAMP()
);               
ALTER TABLE BROKER.CLIENT_OUT_MSGS ADD CONSTRAINT BROKER.CONSTRAINT_6 PRIMARY KEY(CLIENT_ID, MSG_ID);         
-- 0 +/- SELECT COUNT(*) FROM BROKER.CLIENT_OUT_MSGS;         
CREATE CACHED TABLE BROKER.RETAINED_MSGS(
    TOPIC VARCHAR(512) NOT NULL,
    MSG_ID CHAR(36) NOT NULL
); 
ALTER TABLE BROKER.RETAINED_MSGS ADD CONSTRAINT BROKER.CONSTRAINT_4 PRIMARY KEY(TOPIC);       
-- 0 +/- SELECT COUNT(*) FROM BROKER.RETAINED_MSGS;           
CREATE CACHED TABLE BROKER.SUBSCRIPTIONS(
    CLIENT_ID VARCHAR(128) NOT NULL,
    TOPIC VARCHAR(512) NOT NULL,
    QOS TINYINT DEFAULT NULL
);           
ALTER TABLE BROKER.SUBSCRIPTIONS ADD CONSTRAINT BROKER.CONSTRAINT_3 PRIMARY KEY(CLIENT_ID, TOPIC);            
-- 0 +/- SELECT COUNT(*) FROM BROKER.SUBSCRIPTIONS;           
ALTER TABLE BROKER.CLIENTS ADD CONSTRAINT BROKER.CLIENT_WILL_MSG FOREIGN KEY(WILL_MSG) REFERENCES BROKER.MSGS(MSG_ID) ON DELETE CASCADE NOCHECK;              
ALTER TABLE BROKER.CLIENT_OUT_MSGS ADD CONSTRAINT BROKER.CLIENT_OUT_MSGS_CLIENT_ID FOREIGN KEY(CLIENT_ID) REFERENCES BROKER.CLIENTS(CLIENT_ID) ON DELETE CASCADE ON UPDATE CASCADE NOCHECK;   
ALTER TABLE BROKER.CLIENT_OUT_MSGS ADD CONSTRAINT BROKER.CLIENT_OUT_MSGS_MSG_ID FOREIGN KEY(MSG_ID) REFERENCES BROKER.MSGS(MSG_ID) ON DELETE CASCADE ON UPDATE CASCADE NOCHECK;               
ALTER TABLE BROKER.CLIENT_IN_MSGS ADD CONSTRAINT BROKER.CLIENT_IN_MSGS_CLIENT_ID FOREIGN KEY(CLIENT_ID) REFERENCES BROKER.CLIENTS(CLIENT_ID) ON DELETE CASCADE ON UPDATE CASCADE NOCHECK;     
ALTER TABLE BROKER.CLIENT_IN_MSGS ADD CONSTRAINT BROKER.CLIENT_IN_MSGS_MSG_ID FOREIGN KEY(MSG_ID) REFERENCES BROKER.MSGS(MSG_ID) ON DELETE CASCADE ON UPDATE CASCADE NOCHECK; 
ALTER TABLE BROKER.RETAINED_MSGS ADD CONSTRAINT BROKER.RETAINED_MSGS_MSG_ID FOREIGN KEY(MSG_ID) REFERENCES BROKER.MSGS(MSG_ID) ON DELETE CASCADE ON UPDATE CASCADE NOCHECK;   
