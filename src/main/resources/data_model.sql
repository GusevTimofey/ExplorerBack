DROP SCHEMA public CASCADE;
CREATE SCHEMA public;

CREATE TABLE HEADERS
(
    id                VARCHAR(64) PRIMARY KEY,
    parent_id         VARCHAR(64)   NOT NULL,
    txs_root          VARCHAR(64)   NOT NULL,
    state_root        VARCHAR(64)   NOT NULL,
    version           SMALLINT      NOT NULL,
    height            INTEGER       NOT NULL,
    difficulty        BIGINT        NOT NULL,
    timestamp         BIGINT        NOT NULL,
    nonce             BIGINT        NOT NULL,
    equihash_solution INTEGER ARRAY NOT NULL,
    txs_count         INTEGER DEFAULT 1,
    miner_address     VARCHAR(64)   NOT NULL,
    is_in_best_chain  BOOLEAN       NOT NULL
);

CREATE INDEX "header_height_index" ON HEADERS (id); /* Used for faster iteration around the height */
CREATE INDEX "header_parent_id_index" ON HEADERS (parent_id); /* Used for faster chain iteration */

CREATE TABLE TRANSACTIONS
(
    id             VARCHAR(64),
    header_id      VARCHAR(64) NOT NULL REFERENCES HEADERS (id),
    fee            BIGINT      NOT NULL,
    timestamp      BIGINT      NOT NULL,
    proof          TEXT,
    is_coinbase_tx BOOLEAN     NOT NULL,
    PRIMARY KEY (id, header_id) /* Composite key is used because the same transaction may exist in different blocks */
);

CREATE INDEX "transaction_header_id_index" ON TRANSACTIONS (header_id);

CREATE TABLE INPUTS
(
    tx_id    VARCHAR(64) REFERENCES TRANSACTIONS (id),
    box_id   VARCHAR(64),
    proofs   TEXT NOT NULL,
    contract TEXT NOT NULL,
    PRIMARY KEY (tx_id, box_id) /* Composite key is used because the same input may exist in different transactions */
);

CREATE INDEX "input_transaction_id_index" ON INPUTS (tx_id);

CREATE TABLE OUTPUTS
(
    id             VARCHAR(64) PRIMARY KEY,
    tx_id          VARCHAR(64) REFERENCES TRANSACTIONS (id),
    output_type_id SMALLINT    NOT NULL,
    contract_hash  VARCHAR(64) NOT NULL REFERENCES WALLETS (contract_hash),
    is_active      BOOLEAN     NOT NULL,
    nonce          BIGINT      NOT NULL,
    amount         BIGINT,
    data           TEXT,
    token_id       TEXT
);

CREATE INDEX "outputs_tx_id_index" ON OUTPUTS (tx_id);
CREATE INDEX "outputs_is_active" ON OUTPUTS (is_active);

CREATE TABLE WALLETS
(
    contract_hash VARCHAR(64),
    token_id      VARCHAR(64),
    balance       BIGINT NOT NULL,
    PRIMARY KEY (contract_hash, token_id)
);

CREATE INDEX "wallets_contract_hash" ON WALLETS (contract_hash);
CREATE INDEX "wallets_contract_hash" ON WALLETS (token_id);