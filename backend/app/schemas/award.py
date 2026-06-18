from pydantic import BaseModel, Field
from typing import Optional
from datetime import datetime
from .user import User


class AwardBase(BaseModel):
    score: float = Field(..., ge=0, le=100)
    month: str  # Format: YYYY-MM


class AwardCreate(AwardBase):
    user_id: int


class AwardUpdate(BaseModel):
    score: Optional[float] = Field(None, ge=0, le=100)
    month: Optional[str] = None


class AwardInDBBase(AwardBase):
    id: int
    user_id: int
    created_at: datetime

    class Config:
        from_attributes = True


class Award(AwardInDBBase):
    pass
