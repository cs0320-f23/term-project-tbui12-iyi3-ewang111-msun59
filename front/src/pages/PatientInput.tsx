import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  buildTrie,
  findSuggestions,
  collectSuggestions,
} from "../components/trie";

const PatientInput: React.FC = () => {
  const navigate = useNavigate();

  const handleClickBack = () => {
    navigate("/");
  };

  const [activeIngredients, setActiveIngredients] = useState("");
  const [allergies, setAllergies] = useState("");
  const [additionalFields, setAdditionalFields] = useState("");
  const [activeIngredientsSuggestions, setActiveIngredientsSuggestions] =
    useState<string[]>([]);
  const [allergiesSuggestions, setAllergiesSuggestions] = useState<string[]>(
    []
  );
  const [additionalFieldsSuggestions, setAdditionalFieldsSuggestions] =
    useState<string[]>([]);

  const inputRef = useRef<HTMLInputElement>(null);

  const dummySuggestions = [
    "Ingredient1",
    "Ingredient2",
    "Ingredient3",
    "Ingredient4",
    "apple",
    "aloha",
    "balls",
    "banana",
    "cat",
    "cute",
  ];

  const trie = buildTrie(dummySuggestions);

  const handleInputChange = (
    event: React.ChangeEvent<HTMLInputElement>,
    setFunction: React.Dispatch<React.SetStateAction<string>>,
    suggestionsFunction: React.Dispatch<React.SetStateAction<string[]>>
  ) => {
    const userInput = event.target.value;
    setFunction(userInput);

    const suggestions = findSuggestions(trie, userInput);
    suggestionsFunction(suggestions);
  };

  const handleSuggestionClick = (
    suggestion: string,
    setFunction: React.Dispatch<React.SetStateAction<string>>,
    setSuggestionsFunction: React.Dispatch<React.SetStateAction<string[]>>
  ) => {
    setFunction(suggestion);
    setSuggestionsFunction([]); // Clear suggestions when a suggestion is clicked
  };

  const handleSubmit = () => {
    console.log("Active Ingredients:", activeIngredients);
    console.log("Allergies:", allergies);
    console.log("Additional Fields:", additionalFields);

    navigate("/patientoutput", {
      state: {
        activeIngredients,
        allergies,
        additionalFields,
      },
    });

    setActiveIngredients("");
    setAllergies("");
    setAdditionalFields("");
  };

  const handleClickOutside = (event: MouseEvent) => {
    if (inputRef.current && !inputRef.current.contains(event.target as Node)) {
      // Click is outside of the input field, hide suggestions
      setActiveIngredientsSuggestions([]);
    }
  };

  useEffect(() => {
    // Attach click event listener to the document body
    document.body.addEventListener("click", handleClickOutside);

    // Cleanup the event listener on component unmount
    return () => {
      document.body.removeEventListener("click", handleClickOutside);
    };
  }, []); // Empty dependency array ensures that the effect runs once on mount

  return (
    <div className="form-body">
      <div className="form-side"></div>
      <div className="form-patient-image"></div>
      <div className="form-text">
        <div className="text-line">Welcome Patients</div>
      </div>
      <div className="form-container">
        <div className="form">
          <div className="suggestions-container">
            <input
              type="text"
              className="form-control"
              placeholder="Active Ingredients"
              value={activeIngredients}
              onChange={(e) =>
                handleInputChange(
                  e,
                  setActiveIngredients,
                  setActiveIngredientsSuggestions
                )
              }
              ref={inputRef}
            />
            {activeIngredientsSuggestions.length > 0 && (
              <ul>
                {activeIngredientsSuggestions.map((suggestion) => (
                  <li
                    key={suggestion}
                    onClick={() =>
                      handleSuggestionClick(
                        suggestion,
                        setActiveIngredients,
                        setActiveIngredientsSuggestions
                      )
                    }
                  >
                    {suggestion}
                  </li>
                ))}
              </ul>
            )}
          </div>
          <div className="suggestions-container">
            <input
              type="text"
              className="form-control"
              placeholder="Allergies"
              value={allergies}
              onChange={(e) =>
                handleInputChange(e, setAllergies, setAllergiesSuggestions)
              }
            />
            {allergiesSuggestions.length > 0 && (
              <ul>
                {allergiesSuggestions.map((suggestion) => (
                  <li
                    key={suggestion}
                    onClick={() =>
                      handleSuggestionClick(
                        suggestion,
                        setAllergies,
                        setAllergiesSuggestions
                      )
                    }
                  >
                    {suggestion}
                  </li>
                ))}
              </ul>
            )}
          </div>
          <div className="suggestions-container">
            <input
              type="text"
              className="form-control"
              placeholder="Additional Fields"
              value={additionalFields}
              onChange={(e) =>
                handleInputChange(
                  e,
                  setAdditionalFields,
                  setAdditionalFieldsSuggestions
                )
              }
            />
            {additionalFieldsSuggestions.length > 0 && (
              <ul>
                {additionalFieldsSuggestions.map((suggestion) => (
                  <li
                    key={suggestion}
                    onClick={() =>
                      handleSuggestionClick(
                        suggestion,
                        setAdditionalFields,
                        setAdditionalFieldsSuggestions
                      )
                    }
                  >
                    {suggestion}
                  </li>
                ))}
              </ul>
            )}
          </div>
          <div className="input-button-container">
            <button className="form-button" onClick={handleClickBack}>
              Back
            </button>
            <button className="form-button" onClick={handleSubmit}>
              Submit
            </button>
            <div className="input-message">Yo</div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default PatientInput;
