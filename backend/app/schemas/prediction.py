from pydantic import BaseModel, Field
from typing import Optional
from datetime import datetime
from app.models.prediction import PredictionOutcome


class PredictionBase(BaseModel):
    title: str = Field(..., min_length=1, max_length=200)
    description: Optional[str] = None
    deadline: datetime


class PredictionCreate(PredictionBase):
    pass


class PredictionUpdate(BaseModel):
    title: Optional[str] = Field(None, min_length=1, max_length=200)
    description: Optional[str] = None
    deadline: Optional[datetime] = None
    outcome: Optional[PredictionOutcome] = None


class PredictionInDBBase(PredictionBase):
    id: int
    outcome: Optional[PredictionOutcome] = None
    resolved_at: Optional[datetime] = None
    created_at: datetime
    created_by: int

    class Config:
        from_attributes = True


class Prediction(PredictionInDBBase):
    pass
