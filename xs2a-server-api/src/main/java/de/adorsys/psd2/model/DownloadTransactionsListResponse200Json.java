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

package de.adorsys.psd2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import java.io.OutputStream;
import java.util.Objects;

/**
 * Stream which contents the list of account transactions for a successful download transaction list request. 
 */
@ApiModel(description = "Stream which contents the list of account transactions for a successful download transaction list request. ")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2019-07-22T12:24:04.488+03:00[Europe/Kiev]")

public class DownloadTransactionsListResponse200Json   {
  @JsonProperty("outputStream")
  private OutputStream outputStream = null;

  public DownloadTransactionsListResponse200Json resource(OutputStream outputStream) {
    this.outputStream = outputStream;
    return this;
  }

  /**
   * Get resource
   * @return resource
  **/
  @ApiModelProperty(value = "")

  @Valid


  @JsonProperty("outputStream")
  public OutputStream getOutputStream() {
    return outputStream;
  }

  public void setOutputStream(OutputStream outputStream) {
    this.outputStream = outputStream;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DownloadTransactionsListResponse200Json downloadTransactionsListResponse200Json = (DownloadTransactionsListResponse200Json) o;
    return Objects.equals(this.outputStream, downloadTransactionsListResponse200Json.outputStream);
  }

  @Override
  public int hashCode() {
    return Objects.hash(outputStream);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DownloadTransactionsListResponse200Json {\n");
    
    sb.append("    outputStream: ").append(toIndentedString(outputStream)).append("\n");
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

