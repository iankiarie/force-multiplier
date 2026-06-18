from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.routers import auth, predictions, bets, transactions, awards, notes

app = FastAPI(
    title="ForceMultiplier API",
    description="Backend for the ForceMultiplier workplace gamification app.",
    version="0.1.0",
)

# CORS middleware - adjust origins as needed for your frontend
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # In production, replace with specific origins
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routers
app.include_router(auth.router, prefix="/auth", tags=["auth"])
app.include_router(predictions.router, prefix="/predictions", tags=["predictions"])
app.include_router(bets.router, prefix="/bets", tags=["bets"])
app.include_router(transactions.router, prefix="/transactions", tags=["transactions"])
app.include_router(awards.router, prefix="/awards", tags=["awards"])
app.include_router(notes.router, prefix="/notes", tags=["notes"])

# Root endpoint
@app.get("/")
def read_root():
    return {"message": "Welcome to the ForceMultiplier API!"}