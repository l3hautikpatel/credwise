import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.preprocessing import StandardScaler, LabelEncoder
from sklearn.impute import SimpleImputer
from sklearn.pipeline import Pipeline
from sklearn.ensemble import RandomForestClassifier, GradientBoostingRegressor
from sklearn.metrics import mean_absolute_error, accuracy_score, classification_report, mean_squared_error
from sklearn.feature_selection import mutual_info_regression
import pickle

class ImprovedLineOfCreditApprovalModel:
    def __init__(self, data_path=None):
        if data_path:
            self.data = pd.read_csv(data_path)
            self.prepare_data()
        else:
            self.data = None

    def prepare_data(self):
        categorical_cols = ['province', 'employment_status', 'payment_history']
        self.le_dict = {}
        for col in categorical_cols:
            le = LabelEncoder()
            self.data[col] = le.fit_transform(self.data[col].astype(str))
            self.le_dict[col] = le
        
        self.features = [
            'age', 'province', 'employment_status', 'months_employed',
            'annual_income', 'self_reported_debt', 'self_reported_expenses',
            'total_credit_limit', 'credit_utilization', 'num_open_accounts',
            'num_credit_inquiries', 'monthly_expenses', 'dti',
            'payment_history', 'requested_amount', 'estimated_debt'
        ]
        
        self.data['log_annual_income'] = np.log1p(self.data['annual_income'])
        self.data['log_approved_amount'] = np.log1p(self.data['approved_amount'])
        
        for col in ['requested_amount', 'estimated_debt']:
            if col not in self.data.columns:
                self.data[col] = 0
        
        X = self.data[self.features]
        self.imputer = SimpleImputer(strategy='median')
        X_imputed = self.imputer.fit_transform(X)
        self.scaler = StandardScaler()
        self.X_scaled = self.scaler.fit_transform(X_imputed)
        
        self.y_credit_score = self.data['credit_score']
        self.y_approved = self.data['approved']
        self.y_approved_amount = self.data['approved_amount']
        self.y_log_approved_amount = self.data['log_approved_amount']
        self.y_interest_rate = self.data['interest_rate']

    def predict_loan_eligibility(self, applicant_data):
        # Ensure categorical columns are transformed
        categorical_cols = ['province', 'employment_status', 'payment_history']
        applicant_df = pd.DataFrame([applicant_data])
        
        for col in categorical_cols:
            if col in applicant_df.columns:
                le = self.le_dict[col]
                applicant_df[col] = le.transform(applicant_df[col].astype(str))
        
        # Fill missing features with median values
        for feature in self.features:
            if feature not in applicant_df.columns:
                applicant_df[feature] = np.median(self.data[feature]) if self.data is not None else 0
        
        # Scale the input
        X_applicant = self.scaler.transform(self.imputer.transform(applicant_df[self.features]))
        
        # Predict credit score
        predicted_credit_score = self.credit_score_model.predict(X_applicant)[0]
        
        # Predict approval
        approval_prob = self.approval_model.predict_proba(X_applicant)[0]
        is_approved = self.approval_model.predict(X_applicant)[0]
        
        # Predict approved amount and interest rate if approved
        if is_approved:
            log_amount = self.approved_amount_model.predict(X_applicant)[0]
            approved_amount = np.expm1(log_amount)
            interest_rate = self.interest_rate_model.predict(X_applicant)[0]
        else:
            approved_amount = 0
            interest_rate = None
        
        return {
            'predicted_credit_score': round(predicted_credit_score, 2),
            'approval_probability': round(approval_prob[1], 4),
            'is_approved': bool(is_approved),
            'approved_amount': round(approved_amount, 2),
            'interest_rate': round(interest_rate, 2) if interest_rate is not None else None
        }

    def save(self, filepath):
        with open(filepath, 'wb') as f:
            pickle.dump(self, f)
        print(f"✅ Model saved to {filepath}")

    @staticmethod
    def load(filepath):
        with open(filepath, 'rb') as f:
            model = pickle.load(f)
        print(f"✅ Model loaded from {filepath}")
        return model