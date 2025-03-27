from pydantic import BaseModel, Field, validator
from typing import Optional

class CreditQuery(BaseModel):
    self_reported_expenses: int
    credit_score: int
    annual_income: int
    self_reported_debt: int
    requested_amount: int
    age: int
    province: str
    employment_status: str
    months_employed: int
    credit_utilization: float
    num_open_accounts: int
    num_credit_inquiries: int
    payment_history: str
    current_credit_limit: int
    monthly_expenses: int
    estimated_debt: int

    # Optional validators for extra validation
    @validator('province')
    def validate_province(cls, v):
        valid_provinces = ['ON', 'QC', 'BC', 'AB', 'MB', 'SK', 'NS', 'NB', 'NL', 'PE', 'YT', 'NT', 'NU']
        if v.upper() not in valid_provinces:
            raise ValueError(f'Invalid province. Must be one of {valid_provinces}')
        return v.upper()

    @validator('employment_status')
    def validate_employment_status(cls, v):
        valid_statuses = ['Full-time', 'Part-time', 'Unemployed', 'Self-employed', 'Student']
        if v not in valid_statuses:
            raise ValueError(f'Invalid employment status. Must be one of {valid_statuses}')
        return v

    @validator('payment_history')
    def validate_payment_history(cls, v):
        valid_histories = ['On Time', 'Late', 'Default']
        if v not in valid_histories:
            raise ValueError(f'Invalid payment history. Must be one of {valid_histories}')
        return v