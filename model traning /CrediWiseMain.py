from fastapi import FastAPI
from pydantic import BaseModel
from typing import Literal
from model_loader import model

app = FastAPI()

class ApplicantData(BaseModel):
    age: int
    province: str
    employment_status: str
    months_employed: int
    annual_income: float
    self_reported_debt: float
    self_reported_expenses: float
    total_credit_limit: float
    credit_utilization: float
    num_open_accounts: int
    num_credit_inquiries: int
    monthly_expenses: float
    dti: float
    payment_history: Literal["On Time", "Late", "Default"]
    requested_amount: float
    estimated_debt: float

@app.post("/predict")
def predict_loan_eligibility(applicant: ApplicantData):
    input_data = applicant.dict()
    result = model.predict_loan_eligibility(input_data)
    return result
