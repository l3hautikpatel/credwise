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
    def __init__(self, data_path):
        self.data = pd.read_csv(data_path)
        self.prepare_data()

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

    def feature_importance(self, X, y):
        importances = mutual_info_regression(X, y)
        feature_importance = pd.Series(importances, index=self.features)
        return feature_importance.sort_values(ascending=False)

    def train_credit_score_model(self):
        mask = ~self.y_credit_score.isna()
        X_train, X_test, y_train, y_test = train_test_split(
            self.X_scaled[mask], self.y_credit_score[mask], test_size=0.2, random_state=42
        )

        pipeline = Pipeline([
            ('imputer', SimpleImputer(strategy='median')),
            ('regressor', GradientBoostingRegressor(n_estimators=300, max_depth=6, learning_rate=0.05, random_state=42))
        ])

        pipeline.fit(X_train, y_train)
        y_pred = pipeline.predict(X_test)

        print("Credit Score Prediction:")
        print(f"MAE: {mean_absolute_error(y_test, y_pred):.2f}")
        print(f"RMSE: {np.sqrt(mean_squared_error(y_test, y_pred)):.2f}")
        print("\nTop 5 Important Features:")
        print(self.feature_importance(X_train, y_train).head())

        self.credit_score_model = pipeline
        return pipeline

    def train_approval_model(self):
        mask = ~self.y_approved.isna()
        X_train, X_test, y_train, y_test = train_test_split(
            self.X_scaled[mask], self.y_approved[mask], test_size=0.2, random_state=42
        )

        pipeline = Pipeline([
            ('imputer', SimpleImputer(strategy='median')),
            ('classifier',
             RandomForestClassifier(n_estimators=300, class_weight='balanced', max_depth=10, min_samples_split=5,
                                    random_state=42))
        ])

        pipeline.fit(X_train, y_train)
        y_pred = pipeline.predict(X_test)

        print("\nApproval Prediction:")
        print(f"Accuracy: {accuracy_score(y_test, y_pred):.2f}")
        print("\nClassification Report:")
        print(classification_report(y_test, y_pred))

        cv_scores = cross_val_score(pipeline, self.X_scaled[mask], self.y_approved[mask], cv=5)
        print(f"\nCross-validation Scores: {cv_scores}")
        print(f"Mean CV Score: {cv_scores.mean():.2f}")

        print("\nTop 5 Important Features:")
        print(self.feature_importance(X_train, y_train).head())

        self.approval_model = pipeline
        return pipeline

    def train_approved_amount_model(self):
        approved_mask = (self.y_approved == 1) & ~self.y_approved_amount.isna()
        X_train, X_test, y_train, y_test = train_test_split(
            self.X_scaled[approved_mask], self.y_log_approved_amount[approved_mask], test_size=0.2, random_state=42
        )

        pipeline = Pipeline([
            ('imputer', SimpleImputer(strategy='median')),
            ('regressor', GradientBoostingRegressor(n_estimators=300, max_depth=5, learning_rate=0.05, random_state=42))
        ])

        pipeline.fit(X_train, y_train)
        y_pred_log = pipeline.predict(X_test)
        y_pred = np.expm1(y_pred_log)
        y_true = np.expm1(y_test)

        print("\nApproved Amount Prediction:")
        print(f"MAE: {mean_absolute_error(y_true, y_pred):.2f}")
        print(f"RMSE: {np.sqrt(mean_squared_error(y_true, y_pred)):.2f}")
        print("\nTop 5 Important Features:")
        print(self.feature_importance(X_train, y_train).head())

        self.approved_amount_model = pipeline
        return pipeline

    def train_interest_rate_model(self):
        approved_mask = (self.y_approved == 1) & ~self.y_interest_rate.isna()
        X_train, X_test, y_train, y_test = train_test_split(
            self.X_scaled[approved_mask], self.y_interest_rate[approved_mask], test_size=0.2, random_state=42
        )

        pipeline = Pipeline([
            ('imputer', SimpleImputer(strategy='median')),
            ('regressor', GradientBoostingRegressor(n_estimators=300, max_depth=5, learning_rate=0.05, random_state=42))
        ])

        pipeline.fit(X_train, y_train)
        y_pred = pipeline.predict(X_test)

        print("\nInterest Rate Prediction:")
        print(f"MAE: {mean_absolute_error(y_test, y_pred):.2f}")
        print(f"RMSE: {np.sqrt(mean_squared_error(y_test, y_pred)):.2f}")
        print("\nTop 5 Important Features:")
        print(self.feature_importance(X_train, y_train).head())

        self.interest_rate_model = pipeline
        return pipeline

    def predict_loan_eligibility(self, applicant_data):
        applicant_df = pd.DataFrame([applicant_data])
        categorical_cols = ['province', 'employment_status', 'payment_history']
        for col in categorical_cols:
            if col in applicant_df.columns:
                le = self.le_dict[col]
                applicant_df[col] = le.transform(applicant_df[col].astype(str))

        for feature in self.features:
            if feature not in applicant_df.columns:
                applicant_df[feature] = np.median(self.data[feature])

        X_applicant = self.scaler.transform(self.imputer.transform(applicant_df[self.features]))

        predicted_credit_score = self.credit_score_model.predict(X_applicant)[0]
        approval_prob = self.approval_model.predict_proba(X_applicant)[0]
        is_approved = self.approval_model.predict(X_applicant)[0]

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


