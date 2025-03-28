from pydantic import BaseModel, Field, validator
from typing import Literal
import numpy as np

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

    @validator('credit_utilization', 'dti')
    def validate_percentage(cls, v):
        if v < 0 or v > 100:
            raise ValueError('Must be a percentage between 0 and 100')
        return v

class LoanEligibilityResponse(BaseModel):
    predicted_credit_score: float
    approval_probability: float
    is_approved: bool
    approved_amount: float
    interest_rate: float | None