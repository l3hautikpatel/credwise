import os
import sys

# Ensure the current directory is in the Python path
current_dir = os.path.dirname(os.path.abspath(__file__))
sys.path.append(current_dir)

from model_def import ImprovedLineOfCreditApprovalModel

def load_model(filepath):
    """
    Load the pre-trained model from a pickle file
    
    Args:
        filepath (str): Path to the pickle file containing the model
    
    Returns:
        Loaded model object
    """
    # Get the absolute path to ensure correct file loading
    abs_filepath = os.path.join(current_dir, filepath)
    
    try:
        model = ImprovedLineOfCreditApprovalModel.load(abs_filepath)
        return model
    except Exception as e:
        print(f"❌ Error loading model: {e}")
        raise

# Load the model - make sure the path is correct
model = load_model("loc_approval_model.pkl")