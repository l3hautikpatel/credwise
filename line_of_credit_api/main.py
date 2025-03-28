from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import Literal
import logging
import sys
import os

# Add the current directory to Python path
current_dir = os.path.dirname(os.path.abspath(__file__))
sys.path.append(current_dir)

# Import model loader
from model_loader import model

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Pydantic model for input validation
class ApplicantData(BaseModel):
    age: int = Field(..., ge=18, le=100, description="Applicant's age")
    province: str = Field(..., description="Province of residence")
    employment_status: Literal["Employed", "Self-Employed", "Unemployed", "Student"] = Field(..., description="Current employment status")
    months_employed: int = Field(..., ge=0, description="Months at current job")
    annual_income: float = Field(..., ge=0, description="Annual income")
    self_reported_debt: float = Field(..., ge=0, description="Self-reported total debt")
    self_reported_expenses: float = Field(..., ge=0, description="Self-reported monthly expenses")
    total_credit_limit: float = Field(..., ge=0, description="Total credit limit across all accounts")
    credit_utilization: float = Field(..., ge=0, le=100, description="Credit utilization percentage")
    num_open_accounts: int = Field(..., ge=0, description="Number of open credit accounts")
    num_credit_inquiries: int = Field(..., ge=0, description="Number of recent credit inquiries")
    monthly_expenses: float = Field(..., ge=0, description="Monthly expenses")
    dti: float = Field(..., ge=0, le=100, description="Debt-to-Income ratio")
    payment_history: Literal["On Time", "Late", "Default"] = Field(..., description="Payment history")
    requested_amount: float = Field(..., ge=0, description="Requested line of credit amount")
    estimated_debt: float = Field(..., ge=0, description="Estimated total debt")

# Create FastAPI app
app = FastAPI(
    title="Line of Credit Approval API",
    description="Machine Learning API for Line of Credit Approval Prediction",
    version="1.0.0"
)

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.post("/predict")
async def predict_loan_eligibility(applicant: ApplicantData):
    """
    Predict loan eligibility based on applicant data
    """
    try:
        # Convert input to dictionary for model prediction
        input_data = applicant.dict()
        
        # Log the incoming request
        logger.info(f"Received prediction request for applicant from {input_data.get('province')}")
        
        # Use the pre-loaded model to predict loan eligibility
        result = model.predict_loan_eligibility(input_data)
        
        return result
    
    except Exception as e:
        # Log the error for debugging
        logger.error(f"Prediction error: {str(e)}")
        
        # Raise HTTP exception for client
        raise HTTPException(status_code=500, detail=str(e))

# Health check endpoint
@app.get("/health")
async def health_check():
    """Simple health check endpoint to verify API is running"""
    return {"status": "healthy", "model_loaded": True}

# Optional: Add a simple documentation endpoint
@app.get("/")
async def root():
    return {
        "message": "Welcome to Line of Credit Approval API",
        "endpoints": {
            "/predict": "Predict loan eligibility",
            "/docs": "API Documentation",
            "/health": "Health check"
        }
    }