from model_def import ImprovedLineOfCreditApprovalModel

# Path to your CSV dataset
csv_path = "loc_synthetic_dataset_sk.csv"

# Path to save the model inside your PyCharm project
pkl_path = "loc_approval_model.pkl"

# Train and save the model
model = ImprovedLineOfCreditApprovalModel(csv_path)
model.train_credit_score_model()
model.train_approval_model()
model.train_approved_amount_model()
model.train_interest_rate_model()
model.save(pkl_path)
