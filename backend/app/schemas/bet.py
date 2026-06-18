from pydantic import BaseModel, Field
from typing import Optional
from datetime import datetime
from app.models.prediction import PredictionOutcome


class BetBase(BaseModel):
    stake: int = Field(..., gt=0)
    chosen_outcome: PredictionOutcome


class BetCreate(BetBase):
    prediction_id: int


class BetUpdate(BaseModel):
    stake: Optional[int] = Field(None, gt=0)
    chosen_outcome: Optional[PredictionOutcome] = None
    settled: Optional[bool] = None


class BetInDBBase(BetBase):
    id: int
    settled: bool
    created_at: datetime
    settled_at: Optional[datetime] = None
    user_id: int
    prediction_id: int

    class Config:
        from_attributes = True


class Bet(BetInDBBase):
    pass
