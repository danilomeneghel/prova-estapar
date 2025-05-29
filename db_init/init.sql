DO
$$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'estapar') THEN
      CREATE DATABASE estapar;
   END IF;
END
$$;