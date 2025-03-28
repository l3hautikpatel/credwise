# Line of Credit Approval API

## Overview
This is a machine learning-powered API for predicting line of credit approval using FastAPI.

## Features
- Predict credit score
- Estimate loan approval probability
- Determine loan approval status
- Calculate approved loan amount
- Predict interest rate

## Setup and Installation

### Prerequisites
- Python 3.9+
- pip

### Installation
1. Clone the repository
2. Create a virtual environment
```bash
python -m venv venv
source venv/bin/activate  # On Windows, use `venv\Scripts\activate`
```

3. Install dependencies
```bash
pip install -r requirements.txt
```

4. Train and save the model (if not already done)
```bash
python model_def.py
```

### Running the API
```bash
uvicorn main:app --reload
```

## API Endpoints
- `/predict`: POST endpoint for loan eligibility prediction
- `/health`: GET health check endpoint
- `/docs`: Swagger UI documentation

## Model Training
The model is trained on a synthetic dataset using:
- Random Forest Classifier for approval prediction
- Gradient Boosting Regressors for various predictions
- Preprocessing includes:
  * Label Encoding
  * Feature Scaling
  * Imputation

## Contributing
Please read CONTRIBUTING.md for details on our code of conduct and the process for submitting pull requests.

## License
This project is licensed under the MIT License.