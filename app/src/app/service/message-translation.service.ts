import { Injectable } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';
import { Message } from '../model/message.model';

/**
 * Service for translating server messages with parameter interpolation.
 * Uses Transloco for all translation and interpolation logic.
 */
@Injectable({
  providedIn: 'root'
})
export class MessageTranslationService {

  constructor(private translocoService: TranslocoService) {}

  /**
   * Translates a message using the message-key and parameters.
   * Transloco handles nested keys (e.g., "errors.unit.cyclicDependency") 
   * and parameter interpolation (e.g., {{baseUnitSymbol}}).
   * 
   * @param msg The message object from the server
   * @returns Translated message string with interpolated parameters
   */
  translateMessage(msg: Message): string {
    const translationKey = msg['message-key'];
    return this.translocoService.translate(translationKey, msg.parameters || {});
  }
}
