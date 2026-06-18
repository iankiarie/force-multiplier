from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from typing import List

from app import crud, schemas
from app.deps import get_db, get_current_active_user

router = APIRouter()


@router.post("/", response_model=schemas.Prediction, status_code=status.HTTP_201_CREATED)
def create_prediction(
    prediction_in: schemas.PredictionCreate,
    db: Session = Depends(get_db),
    current_user: schemas.User = Depends(get_current_active_user)
):
    return crud.create_prediction(db=db, prediction_in=prediction_in, creator_id=current_user.id)


@router.get("/", response_model=List[schemas.Prediction])
def read_predictions(
    skip: int = 0,
    limit: int = 100,
    db: Session = Depends(get_db),
    current_user: schemas.User = Depends(get_current_active_user)
):
    # For now, we return all predictions. Later we might want to filter by active or upcoming.
    predictions = crud.get_predictions(db, skip=skip, limit=limit)
    return predictions


@router.get("/{prediction_id}", response_model=schemas.Prediction)
def read_prediction(
    prediction_id: int,
    db: Session = Depends(get_db),
    current_user: schemas.User = Depends(get_current_active_user)
):
    prediction = crud.get_prediction(db, prediction_id=prediction_id)
    if not prediction:
        raise HTTPException(status_code=404, detail="Prediction not found")
    return prediction


@router.patch("/{prediction_id}", response_model=schemas.Prediction)
def update_prediction(
    prediction_id: int,
    prediction_in: schemas.PredictionUpdate,
    db: Session = Depends(get_db),
    current_user: schemas.User = Depends(get_current_active_user)
):
    db_prediction = crud.get_prediction(db, prediction_id=prediction_id)
    if not db_prediction:
        raise HTTPException(status_code=404, detail="Prediction not found")
    # Optional: check if the current user is the creator (only creator can update)
    if db_prediction.created_by != current_user.id:
        raise HTTPException(status_code=403, detail="Not enough permissions")
    return crud.update_prediction(db=db, db_prediction=db_prediction, prediction_in=prediction_in)


@router.delete("/{prediction_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_prediction(
    prediction_id: int,
    db: Session = Depends(get_db),
    current_user: schemas.User = Depends(get_current_active_user)
):
    db_prediction = crud.get_prediction(db, prediction_id=prediction_id)
    if not db_prediction:
        raise HTTPException(status_code=404, detail="Prediction not found")
    # Optional: check if the current user is the creator
    if db_prediction.created_by != current_user.id:
        raise HTTPException(status_code=403, detail="Not enough permissions")
    crud.delete_prediction(db=db, prediction_id=prediction_id)
    return None