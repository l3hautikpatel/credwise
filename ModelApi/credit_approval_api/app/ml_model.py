import joblib
import pandas as pd
import numpy as np
from sklearn.preprocessing import StandardScaler

class CreditApprovalPredictor:
    def __init__(self, model_path='ml_models/credit_approval_model.pkl'):
        self.model = joblib.load(model_path)
        self.scaler = StandardScaler()
        
        self.train_columns = [
            'Self_reported_expenses', 'credit_score', 'annual_income',
            'self_reported_debt', 'requested_amount', 'age', 'province',
            'employment_status', 'months_employed', 'credit_utilization',
            'num_open_accounts', 'num_credit_inquiries', 'payment_history',
            'current_credit_limit', 'monthly_expenses', 'estimated_debt',
            'credit_limit', 'interest_rate'
        ]

    def predict(self, query_data):
        query_df = pd.DataFrame([query_data])
        
        query_df = pd.get_dummies(
            query_df, 
            columns=['province', 'employment_status', 'payment_history'], 
            drop_first=True
        )
        
        for col in self.train_columns:
            if col not in query_df.columns:
                query_df[col] = 0
        
        query_df = query_df[self.train_columns]
        
        query_scaled = self.scaler.fit_transform(query_df)
        
        prediction = self.model.predict(query_scaled)
        probabilities = self.model.predict_proba(query_scaled)
        
        return {
            'approval_status': 'Approved' if prediction[0] == 1 else 'Denied',
            'approval_probability': float(probabilities[0][1])
        }