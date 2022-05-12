package datawave.services.query.predict;

import datawave.microservice.querymetric.BaseQueryMetric;

import java.util.Set;

import java.io.Serializable;

/**
 * Created on 7/6/16.
 */
public interface QueryPredictor<T extends BaseQueryMetric> {
    
    Set<BaseQueryMetric.Prediction> predict(T query) throws PredictionException;
    
    class PredictionException extends Exception implements Serializable {
        
        public PredictionException() {
            super();
        }
        
        public PredictionException(String message) {
            super(message);
        }
        
        public PredictionException(String message, Throwable cause) {
            super(message, cause);
        }
        
        public PredictionException(Throwable cause) {
            super(cause);
        }
    }
    
}
