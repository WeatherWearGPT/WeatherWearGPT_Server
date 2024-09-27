CREATE TABLE weather (
  weather_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  weather_text VARCHAR(255),
  temperature DOUBLE,
  relative_humidity INT,
  wind DOUBLE,
  has_precipitation BOOLEAN,
  weather_date TIMESTAMP
);
