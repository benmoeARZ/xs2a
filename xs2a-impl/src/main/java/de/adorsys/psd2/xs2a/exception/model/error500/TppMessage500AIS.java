/*
 * Copyright 2018-2019 adorsys GmbH & Co KG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.adorsys.psd2.xs2a.exception.model.error500;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.adorsys.psd2.model.TppMessageCategory;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Objects;

// TODO remove, when specification provide a class for such error code https://git.adorsys.de/adorsys/xs2a/aspsp-xs2a/issues/634

/**
 * TppMessage500AIS
 */
@Validated
public class TppMessage500AIS {
    @JsonProperty("category")
    private TppMessageCategory category = null;

    @JsonProperty("code")
    private String code = null;

    @JsonProperty("path")
    private String path = null;

    @JsonProperty("text")
    private String text = null;

    public TppMessage500AIS category(TppMessageCategory category) {
        this.category = category;
        return this;
    }

    /**
     * Get category
     *
     * @return category
     **/
    @ApiModelProperty(required = true, value = "")
    @NotNull
    @Valid
    public TppMessageCategory getCategory() {
        return category;
    }

    public void setCategory(TppMessageCategory category) {
        this.category = category;
    }

    public TppMessage500AIS code(String code) {
        this.code = code;
        return this;
    }

    /**
     * Get code
     *
     * @return code
     **/
    @ApiModelProperty(required = true, value = "")
    @NotNull
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public TppMessage500AIS path(String path) {
        this.path = path;
        return this;
    }

    /**
     * Get path
     *
     * @return path
     **/
    @ApiModelProperty(value = "")
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public TppMessage500AIS text(String text) {
        this.text = text;
        return this;
    }

    /**
     * Get text
     *
     * @return text
     **/
    @ApiModelProperty(value = "")
    @Size(max = 512)
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TppMessage500AIS tppMessage500AIS = (TppMessage500AIS) o;
        return Objects.equals(this.category, tppMessage500AIS.category) &&
                   Objects.equals(this.code, tppMessage500AIS.code) &&
                   Objects.equals(this.path, tppMessage500AIS.path) &&
                   Objects.equals(this.text, tppMessage500AIS.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(category, code, path, text);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class TppMessage500AIS {\n");

        sb.append("    category: ").append(toIndentedString(category)).append("\n");
        sb.append("    code: ").append(toIndentedString(code)).append("\n");
        sb.append("    path: ").append(toIndentedString(path)).append("\n");
        sb.append("    text: ").append(toIndentedString(text)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}

