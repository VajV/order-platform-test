CREATE DATABASE auth_db;
CREATE DATABASE inventory_db;
CREATE DATABASE order_db;
CREATE DATABASE user_db;

GRANT ALL PRIVILEGES ON DATABASE auth_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE inventory_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE order_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE user_db TO postgres;

\c inventory_db;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c order_db;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c auth_db;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c user_db;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
