from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from .models import CreditQuery
from .ml_model import CreditApprovalPredictor
import traceback

app = FastAPI(
    title="Credit Approval Prediction API",
    description="API for predicting credit approval",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"]
)

predictor = CreditApprovalPredictor()

@app.exception_handler(ValueError)
async def value_error_handler(request: Request, exc: ValueError):
    return JSONResponse(
        status_code=422,
        content={
            "error": "Validation Error",
            "detail": str(exc)
        }
    )

@app.post("/predict")
def predict_credit_approval(query: CreditQuery):
    try:
        # Convert to dictionary with validated data
        query_dict = query.dict()
        prediction = predictor.predict(query_dict)
        return prediction
    except Exception as e:
        # Log the full traceback for server-side debugging
        print(traceback.format_exc())
        raise HTTPException(status_code=500, detail=f"Internal server error: {str(e)}")