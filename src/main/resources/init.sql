PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS temperature_unit (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    symbol TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS temperature_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    input_value REAL NOT NULL,
    result_value REAL NOT NULL,
    unit_id INTEGER NOT NULL,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (unit_id) REFERENCES temperature_unit(id)
);

CREATE TABLE IF NOT EXISTS measurement_unit (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    domain TEXT NOT NULL CHECK (domain IN ('temperature', 'speed', 'distance', 'time')),
    code TEXT NOT NULL,
    UNIQUE (domain, code)
);

CREATE TABLE IF NOT EXISTS temperature_unit_registry (
    temperature_unit_id INTEGER PRIMARY KEY,
    measurement_unit_id INTEGER NOT NULL UNIQUE,
    FOREIGN KEY (temperature_unit_id) REFERENCES temperature_unit(id),
    FOREIGN KEY (measurement_unit_id) REFERENCES measurement_unit(id)
);

CREATE TABLE IF NOT EXISTS unit_translation (
    unit_id INTEGER NOT NULL,
    lang TEXT NOT NULL CHECK (lang IN ('en', 'fi', 'sv', 'ja', 'ar')),
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    PRIMARY KEY (unit_id, lang),
    FOREIGN KEY (unit_id) REFERENCES measurement_unit(id)
);

CREATE TABLE IF NOT EXISTS speed_unit (
    id INTEGER PRIMARY KEY,
    code TEXT NOT NULL UNIQUE,
    symbol TEXT NOT NULL,
    FOREIGN KEY (id) REFERENCES measurement_unit(id)
);

CREATE TABLE IF NOT EXISTS speed_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    input_value REAL NOT NULL,
    result_value REAL NOT NULL,
    input_unit_id INTEGER NOT NULL,
    result_unit_id INTEGER NOT NULL,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (input_unit_id) REFERENCES speed_unit(id),
    FOREIGN KEY (result_unit_id) REFERENCES speed_unit(id)
);

CREATE TABLE IF NOT EXISTS distance_unit (
    id INTEGER PRIMARY KEY,
    code TEXT NOT NULL UNIQUE,
    symbol TEXT NOT NULL,
    FOREIGN KEY (id) REFERENCES measurement_unit(id)
);

CREATE TABLE IF NOT EXISTS distance_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    input_value REAL NOT NULL,
    result_value REAL NOT NULL,
    input_unit_id INTEGER NOT NULL,
    result_unit_id INTEGER NOT NULL,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (input_unit_id) REFERENCES distance_unit(id),
    FOREIGN KEY (result_unit_id) REFERENCES distance_unit(id)
);

CREATE TABLE IF NOT EXISTS time_unit (
    id INTEGER PRIMARY KEY,
    code TEXT NOT NULL UNIQUE,
    symbol TEXT NOT NULL,
    FOREIGN KEY (id) REFERENCES measurement_unit(id)
);

CREATE TABLE IF NOT EXISTS time_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    input_value REAL NOT NULL,
    result_value REAL NOT NULL,
    input_unit_id INTEGER NOT NULL,
    result_unit_id INTEGER NOT NULL,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (input_unit_id) REFERENCES time_unit(id),
    FOREIGN KEY (result_unit_id) REFERENCES time_unit(id)
);

INSERT OR IGNORE INTO temperature_unit (name, symbol) VALUES
    ('Celsius', 'C'),
    ('Fahrenheit', 'F'),
    ('Kelvin', 'K');

INSERT OR IGNORE INTO measurement_unit (domain, code) VALUES
    ('temperature', 'celsius'),
    ('temperature', 'fahrenheit'),
    ('temperature', 'kelvin'),
    ('speed', 'meters_per_second'),
    ('speed', 'kilometers_per_hour'),
    ('speed', 'miles_per_hour'),
    ('distance', 'meter'),
    ('distance', 'kilometer'),
    ('distance', 'mile'),
    ('time', 'second'),
    ('time', 'minute'),
    ('time', 'hour');

INSERT OR IGNORE INTO temperature_unit_registry (temperature_unit_id, measurement_unit_id)
SELECT t.id, m.id
FROM temperature_unit t
JOIN measurement_unit m
  ON m.domain = 'temperature'
 AND m.code = CASE t.name
     WHEN 'Celsius' THEN 'celsius'
     WHEN 'Fahrenheit' THEN 'fahrenheit'
     WHEN 'Kelvin' THEN 'kelvin'
 END;

INSERT OR IGNORE INTO speed_unit (id, code, symbol)
SELECT id, 'meters_per_second', 'm/s' FROM measurement_unit
WHERE domain = 'speed' AND code = 'meters_per_second';
INSERT OR IGNORE INTO speed_unit (id, code, symbol)
SELECT id, 'kilometers_per_hour', 'km/h' FROM measurement_unit
WHERE domain = 'speed' AND code = 'kilometers_per_hour';
INSERT OR IGNORE INTO speed_unit (id, code, symbol)
SELECT id, 'miles_per_hour', 'mph' FROM measurement_unit
WHERE domain = 'speed' AND code = 'miles_per_hour';

INSERT OR IGNORE INTO distance_unit (id, code, symbol)
SELECT id, 'meter', 'm' FROM measurement_unit
WHERE domain = 'distance' AND code = 'meter';
INSERT OR IGNORE INTO distance_unit (id, code, symbol)
SELECT id, 'kilometer', 'km' FROM measurement_unit
WHERE domain = 'distance' AND code = 'kilometer';
INSERT OR IGNORE INTO distance_unit (id, code, symbol)
SELECT id, 'mile', 'mi' FROM measurement_unit
WHERE domain = 'distance' AND code = 'mile';

INSERT OR IGNORE INTO time_unit (id, code, symbol)
SELECT id, 'second', 's' FROM measurement_unit
WHERE domain = 'time' AND code = 'second';
INSERT OR IGNORE INTO time_unit (id, code, symbol)
SELECT id, 'minute', 'min' FROM measurement_unit
WHERE domain = 'time' AND code = 'minute';
INSERT OR IGNORE INTO time_unit (id, code, symbol)
SELECT id, 'hour', 'h' FROM measurement_unit
WHERE domain = 'time' AND code = 'hour';

INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'en', 'Celsius', 'Temperature scale with water freezing at 0 °C and boiling at 100 °C at standard pressure.'
FROM measurement_unit WHERE domain = 'temperature' AND code = 'celsius';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'fi', 'Celsius-aste', 'Lämpötila-asteikko, jossa vesi jäätyy 0 °C:ssa ja kiehuu 100 °C:ssa normaalipaineessa.'
FROM measurement_unit WHERE domain = 'temperature' AND code = 'celsius';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'sv', 'Celsiusgrad', 'Temperaturskala där vatten fryser vid 0 °C och kokar vid 100 °C vid normalt tryck.'
FROM measurement_unit WHERE domain = 'temperature' AND code = 'celsius';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'ja', 'セルシウス度', '標準気圧で水の凝固点を0 °C、沸点を100 °Cとする温度目盛りです。'
FROM measurement_unit WHERE domain = 'temperature' AND code = 'celsius';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'ar', 'درجة مئوية', 'مقياس حرارة يتجمد فيه الماء عند 0 °م ويغلي عند 100 °م تحت الضغط القياسي.'
FROM measurement_unit WHERE domain = 'temperature' AND code = 'celsius';

INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'en', 'Fahrenheit', 'Temperature scale with water freezing at 32 °F and boiling at 212 °F at standard pressure.'
FROM measurement_unit WHERE domain = 'temperature' AND code = 'fahrenheit';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'fi', 'Fahrenheit-aste', 'Lämpötila-asteikko, jossa vesi jäätyy 32 °F:ssa ja kiehuu 212 °F:ssa normaalipaineessa.'
FROM measurement_unit WHERE domain = 'temperature' AND code = 'fahrenheit';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'sv', 'Fahrenheitgrad', 'Temperaturskala där vatten fryser vid 32 °F och kokar vid 212 °F vid normalt tryck.'
FROM measurement_unit WHERE domain = 'temperature' AND code = 'fahrenheit';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'ja', '華氏度', '標準気圧で水の凝固点を32 °F、沸点を212 °Fとする温度目盛りです。'
FROM measurement_unit WHERE domain = 'temperature' AND code = 'fahrenheit';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'ar', 'درجة فهرنهايت', 'مقياس حرارة يتجمد فيه الماء عند 32 °ف ويغلي عند 212 °ف تحت الضغط القياسي.'
FROM measurement_unit WHERE domain = 'temperature' AND code = 'fahrenheit';

INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'en', 'Kelvin', 'SI unit of thermodynamic temperature, zero kelvin is absolute zero.'
FROM measurement_unit WHERE domain = 'temperature' AND code = 'kelvin';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'fi', 'kelvin', 'Termodynaamisen lämpötilan SI-yksikkö, nolla kelviniä on absoluuttinen nollapiste.'
FROM measurement_unit WHERE domain = 'temperature' AND code = 'kelvin';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'sv', 'kelvin', 'SI-enheten för termodynamisk temperatur, noll kelvin är den absoluta nollpunkten.'
FROM measurement_unit WHERE domain = 'temperature' AND code = 'kelvin';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'ja', 'ケルビン', '熱力学温度のSI単位です。0ケルビンは絶対零度です。'
FROM measurement_unit WHERE domain = 'temperature' AND code = 'kelvin';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'ar', 'كلفن', 'وحدة النظام الدولي لدرجة الحرارة الديناميكية الحرارية؛ الصفر كلفن هو الصفر المطلق.'
FROM measurement_unit WHERE domain = 'temperature' AND code = 'kelvin';

INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'en', 'Meters per second', 'Speed equal to one meter traveled in one second.'
FROM measurement_unit WHERE domain = 'speed' AND code = 'meters_per_second';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'fi', 'metriä sekunnissa', 'Nopeus, jossa kuljetaan yksi metri sekunnissa.'
FROM measurement_unit WHERE domain = 'speed' AND code = 'meters_per_second';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'sv', 'meter per sekund', 'Hastighet där en meter tillryggaläggs på en sekund.'
FROM measurement_unit WHERE domain = 'speed' AND code = 'meters_per_second';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'ja', 'メートル毎秒', '1秒間に1メートル進む速さです。'
FROM measurement_unit WHERE domain = 'speed' AND code = 'meters_per_second';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'ar', 'متر في الثانية', 'سرعة تُقطع فيها مسافة متر واحد خلال ثانية واحدة.'
FROM measurement_unit WHERE domain = 'speed' AND code = 'meters_per_second';

INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'en', 'Kilometers per hour', 'Speed equal to one kilometer traveled in one hour.'
FROM measurement_unit WHERE domain = 'speed' AND code = 'kilometers_per_hour';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'fi', 'kilometriä tunnissa', 'Nopeus, jossa kuljetaan yksi kilometri tunnissa.'
FROM measurement_unit WHERE domain = 'speed' AND code = 'kilometers_per_hour';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'sv', 'kilometer i timmen', 'Hastighet där en kilometer tillryggaläggs på en timme.'
FROM measurement_unit WHERE domain = 'speed' AND code = 'kilometers_per_hour';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'ja', 'キロメートル毎時', '1時間に1キロメートル進む速さです。'
FROM measurement_unit WHERE domain = 'speed' AND code = 'kilometers_per_hour';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'ar', 'كيلومتر في الساعة', 'سرعة تُقطع فيها مسافة كيلومتر واحد خلال ساعة واحدة.'
FROM measurement_unit WHERE domain = 'speed' AND code = 'kilometers_per_hour';

INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'en', 'Miles per hour', 'Speed equal to one international mile traveled in one hour.'
FROM measurement_unit WHERE domain = 'speed' AND code = 'miles_per_hour';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'fi', 'mailia tunnissa', 'Nopeus, jossa kuljetaan yksi kansainvälinen maili tunnissa.'
FROM measurement_unit WHERE domain = 'speed' AND code = 'miles_per_hour';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'sv', 'internationella miles i timmen', 'Hastighet där en internationell mile tillryggaläggs på en timme.'
FROM measurement_unit WHERE domain = 'speed' AND code = 'miles_per_hour';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'ja', 'マイル毎時', '1時間に1国際マイル進む速さです。'
FROM measurement_unit WHERE domain = 'speed' AND code = 'miles_per_hour';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'ar', 'ميل في الساعة', 'سرعة تُقطع فيها مسافة ميل دولي واحد خلال ساعة واحدة.'
FROM measurement_unit WHERE domain = 'speed' AND code = 'miles_per_hour';

INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'en', 'Meter', 'SI base unit of length.'
FROM measurement_unit WHERE domain = 'distance' AND code = 'meter';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'fi', 'metri', 'Pituuden SI-perusyksikkö.'
FROM measurement_unit WHERE domain = 'distance' AND code = 'meter';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'sv', 'meter', 'SI-grundenhet för längd.'
FROM measurement_unit WHERE domain = 'distance' AND code = 'meter';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'ja', 'メートル', '長さのSI基本単位です。'
FROM measurement_unit WHERE domain = 'distance' AND code = 'meter';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'ar', 'متر', 'الوحدة الأساسية للطول في النظام الدولي للوحدات.'
FROM measurement_unit WHERE domain = 'distance' AND code = 'meter';

INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'en', 'Kilometer', 'A length of one thousand meters.'
FROM measurement_unit WHERE domain = 'distance' AND code = 'kilometer';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'fi', 'kilometri', 'Tuhannen metrin pituus.'
FROM measurement_unit WHERE domain = 'distance' AND code = 'kilometer';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'sv', 'kilometer', 'En längd på ettusen meter.'
FROM measurement_unit WHERE domain = 'distance' AND code = 'kilometer';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'ja', 'キロメートル', '1000メートルに等しい長さの単位です。'
FROM measurement_unit WHERE domain = 'distance' AND code = 'kilometer';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'ar', 'كيلومتر', 'وحدة طول تساوي ألف متر.'
FROM measurement_unit WHERE domain = 'distance' AND code = 'kilometer';

INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'en', 'Mile', 'A length equal to 1,609.344 meters (the international mile).'
FROM measurement_unit WHERE domain = 'distance' AND code = 'mile';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'fi', 'maili', 'Pituus, joka vastaa 1 609,344 metriä (kansainvälinen maili).'
FROM measurement_unit WHERE domain = 'distance' AND code = 'mile';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'sv', 'internationell mile', 'En längd som motsvarar 1 609,344 meter (internationell mile).'
FROM measurement_unit WHERE domain = 'distance' AND code = 'mile';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'ja', 'マイル', '1,609.344メートルに相当する国際マイルです。'
FROM measurement_unit WHERE domain = 'distance' AND code = 'mile';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'ar', 'ميل', 'وحدة طول تساوي 1609.344 متر (الميل الدولي).'
FROM measurement_unit WHERE domain = 'distance' AND code = 'mile';

INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'en', 'Second', 'SI base unit of time.'
FROM measurement_unit WHERE domain = 'time' AND code = 'second';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'fi', 'sekunti', 'Ajan SI-perusyksikkö.'
FROM measurement_unit WHERE domain = 'time' AND code = 'second';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'sv', 'sekund', 'SI-grundenhet för tid.'
FROM measurement_unit WHERE domain = 'time' AND code = 'second';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'ja', '秒', '時間のSI基本単位です。'
FROM measurement_unit WHERE domain = 'time' AND code = 'second';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'ar', 'ثانية', 'الوحدة الأساسية للزمن في النظام الدولي للوحدات.'
FROM measurement_unit WHERE domain = 'time' AND code = 'second';

INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'en', 'Minute', 'A duration of sixty seconds.'
FROM measurement_unit WHERE domain = 'time' AND code = 'minute';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'fi', 'minuutti', 'Kuudenkymmenen sekunnin kesto.'
FROM measurement_unit WHERE domain = 'time' AND code = 'minute';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'sv', 'minut', 'En tidsperiod på sextio sekunder.'
FROM measurement_unit WHERE domain = 'time' AND code = 'minute';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'ja', '分', '60秒の時間単位です。'
FROM measurement_unit WHERE domain = 'time' AND code = 'minute';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'ar', 'دقيقة', 'مدة زمنية تساوي ستين ثانية.'
FROM measurement_unit WHERE domain = 'time' AND code = 'minute';

INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'en', 'Hour', 'A duration of sixty minutes.'
FROM measurement_unit WHERE domain = 'time' AND code = 'hour';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'fi', 'tunti', 'Kuudenkymmenen minuutin kesto.'
FROM measurement_unit WHERE domain = 'time' AND code = 'hour';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'sv', 'timme', 'En tidsperiod på sextio minuter.'
FROM measurement_unit WHERE domain = 'time' AND code = 'hour';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'ja', '時間', '60分の時間単位です。'
FROM measurement_unit WHERE domain = 'time' AND code = 'hour';
INSERT OR IGNORE INTO unit_translation (unit_id, lang, name, description)
SELECT id, 'ar', 'ساعة', 'مدة زمنية تساوي ستين دقيقة.'
FROM measurement_unit WHERE domain = 'time' AND code = 'hour';
