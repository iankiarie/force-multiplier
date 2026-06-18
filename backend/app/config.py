from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    # Database
    DATABASE_URL: str = "postgresql://forcemultiplier:forcemultiplier@localhost:5432/forcemultiplier"

    # Security
    SECRET_KEY: str = "your-super-secret-key-here-change-in-production"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60 * 24 * 7  # one week
    ALGORITHM: str = "HS256"

    # Optional: Debug
    DEBUG: bool = True

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")


settings = Settings()